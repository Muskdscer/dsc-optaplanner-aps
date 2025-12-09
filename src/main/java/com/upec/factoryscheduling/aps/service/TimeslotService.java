package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.repository.TimeslotRepository;
import com.upec.factoryscheduling.aps.resquest.ProcedureRequest;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeslotService {

    private TimeslotRepository timeslotRepository;

    @Autowired
    private void setTimeslotRepository(TimeslotRepository timeslotRepository) {
        this.timeslotRepository = timeslotRepository;
    }

    private ProcedureService procedureService;

    @Autowired
    public void setProcedureService(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    private OrderTaskService orderTaskService;

    @Autowired
    public void setOrderTaskService(OrderTaskService orderTaskService) {
        this.orderTaskService = orderTaskService;
    }

    private OrderService orderService;

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Transactional("h2TransactionManager")
    public Timeslot updateTimeslot(ProcedureRequest request) {
//        Order order = orderService.findFirstByOrderNo(request.getOrderNo());
//        Machine machine = machineService.findFirstByMachineNo(request.getMachineNo());
//        Procedure procedure = procedureService.findFirstByOrderNoAndMachineNoAndProcedureNo(request.getOrderNo(),
//                request.getMachineNo(), request.getProcedureNo());
//        MachineMaintenance maintenance = maintenanceService.findFirstByMachineAndDate(machine, request.getDate().toLocalDate());
//        Timeslot timeslot = timeslotRepository.findFirstByOrderAndProcedureAndMachine(order, procedure,
//                machine);
//        timeslot.setMaintenance(maintenance);
//        timeslot.setManual(Boolean.TRUE);
//        return timeslotRepository.save(timeslot);
        return null;
    }

    public FactorySchedulingSolution findAll() {
        List<Timeslot> timeslots = timeslotRepository.findAll();
        FactorySchedulingSolution solution = new FactorySchedulingSolution();
        solution.setTimeslots(timeslots);
        return solution;
    }


    @Transactional("h2TransactionManager")
    public List<Timeslot> saveAll(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    @Transactional("h2TransactionManager")
    public void deleteAll() {
        timeslotRepository.deleteAll();
    }

    @Transactional("h2TransactionManager")
    public List<Timeslot> receiverTimeslot(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    @Transactional("h2TransactionManager")
    public List<Timeslot> saveTimeslot(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    public List<Timeslot> findAllByOrderIn(List<Order> orders) {
        Sort sort = Sort.by(Sort.Direction.DESC, "order", "procedureIndex", "index").ascending();
        return timeslotRepository.findAllByOrderIn(orders, sort);
    }


    @Transactional("h2TransactionManager")
    public void createTimeslot(List<String> taskNos, List<String> procedureIds, double time, int slice) {
        List<Timeslot> timeslots = new ArrayList<>();
        if (!CollectionUtils.isEmpty(taskNos)) {
            timeslots = timeslotRepository.findAllByTask_TaskNoIn(taskNos);
        }
        if (!CollectionUtils.isEmpty(procedureIds)) {
            timeslots = timeslotRepository.findAllByProcedureIdIn(procedureIds);
        }
        // 为每个工序创建分片Timeslot
        for (Timeslot timeslot : timeslots) {
            if (timeslot.getProcedure().getWorkCenterId() == null) {
                log.info("跳过未绑定工作中心的工序: {}", timeslot.getProcedure().getId());
                continue;
            }
            if (timeslot.getStartTime() != null && timeslot.getEndTime() != null) {
                continue;
            }
            if (time >= 0.5 && slice <= 1) {
                timeslotRepository.saveAll(splitTimeslot(timeslot, time));
            }
            if (slice > 1) {
                timeslotRepository.saveAll(splitTimeslot(timeslot, slice));
            }
        }
    }

    private List<Timeslot> splitTimeslot(Timeslot timeslot, double time) {
        time = time * 60;
        List<Timeslot> timeslots = new ArrayList<>();
        int duration = timeslot.getProcedure().getMachineMinutes();
        int index = timeslot.getIndex();
        if (time >= duration) {
            timeslots.add(timeslot);
            return timeslots;
        }
        duration = duration - (int) time;
        timeslot.setDuration((int) time);
        timeslots.add(timeslot);
        while (duration > 0) {
            Timeslot newTimeslot = new Timeslot();
            BeanUtils.copyProperties(timeslot, newTimeslot);
            index++;
            newTimeslot.setId(timeslot.getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() + "_" + index);
            newTimeslot.setDuration(Math.min(duration, (int) time));
            newTimeslot.setIndex(index);
            timeslots.add(newTimeslot);
            duration = duration - (int) time;
        }
        int total = timeslots.size();
        return timeslots.stream().peek(t -> t.setTotal(total)).collect(Collectors.toList());
    }

    private List<Timeslot> splitTimeslot(Timeslot timeslot, int slice) {
        List<Timeslot> timeslots = new ArrayList<>();
        int duration = timeslot.getProcedure().getMachineMinutes();
        int index = timeslot.getIndex();
        int interval = Math.round((float) duration / slice * 100) / 100;
        timeslot.setDuration(interval);
        timeslots.add(timeslot);
        for (int i = 1; i < slice; i++) {
            Timeslot newTimeslot = new Timeslot();
            BeanUtils.copyProperties(timeslot, newTimeslot);
            index++;
            newTimeslot.setId(timeslot.getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() + "_" + index);
            newTimeslot.setIndex(index);
            newTimeslot.setDuration(Math.min(duration - (interval * i), interval));
            timeslots.add(newTimeslot);
        }
        int total = timeslots.size();
        return timeslots.stream().peek(t -> t.setTotal(total)).collect(Collectors.toList());
    }
}
