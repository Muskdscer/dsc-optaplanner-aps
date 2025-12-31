package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Procedure;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeslotService {

    private TimeslotRepository timeslotRepository;

    @Autowired
    private void setTimeslotRepository(TimeslotRepository timeslotRepository) {
        this.timeslotRepository = timeslotRepository;
    }

    @Transactional("oracleTransactionManager")
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


    @Transactional("oracleTransactionManager")
    public List<Timeslot> saveAll(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    @Transactional("oracleTransactionManager")
    public void deleteAll() {
        timeslotRepository.deleteAll();
    }

    @Transactional("oracleTransactionManager")
    public List<Timeslot> saveTimeslot(List<Timeslot> timeslots) {
        return timeslotRepository.saveAll(timeslots);
    }

    public List<Timeslot> findAllByTaskIn(List<String> taskNos) {
        Sort sort = Sort.by(Sort.Direction.DESC,  "procedureIndex", "index").ascending();
        return timeslotRepository.findAllByProcedure_Task_TaskNoIsIn(taskNos, sort);
    }

    @Transactional("oracleTransactionManager")
    public void createTimeslot(List<String> taskNos, List<String> timeslotIds, double time, int slice) {
        List<Timeslot> timeslots = new ArrayList<>();
        if (!CollectionUtils.isEmpty(taskNos)) {
            timeslots = timeslotRepository.findAllByProcedure_Task_TaskNoIsIn(taskNos);
        }
        if (!CollectionUtils.isEmpty(timeslotIds)) {
            timeslots = timeslotRepository.findAllByIdIsIn(timeslotIds);
        }
        // 为每个工序创建分片Timeslot
        for (Timeslot timeslot : timeslots) {
            if (timeslot.getProcedure().getWorkCenter() == null) {
                log.info("跳过未绑定工作中心的工序: {}", timeslot.getProcedure().getId());
                continue;
            }
            if (timeslot.isManual()) {
                continue;
            }
            List<Timeslot> newTimeslots = new ArrayList<>();
            List<Timeslot> list = timeslotRepository.findAllByProcedureAndIdNot(timeslot.getProcedure(), timeslot.getId());
            if (time >= 0.5 && slice <= 1) {
                newTimeslots.addAll(splitTimeslot(timeslot, list, time));
            }
            if (slice > 1) {
                newTimeslots.addAll(splitTimeslot(timeslot, list, slice));
            }
            timeslotRepository.saveAll(newTimeslots);
        }
    }

    private List<Timeslot> splitTimeslot(Timeslot timeslot, List<Timeslot> others, double time) {
        time = time * 60;
        List<Timeslot> timeslots = new ArrayList<>();
        int duration = timeslot.getDuration();
        int index = timeslot.getTotal();
        if (time >= duration) {
            timeslots.add(timeslot);
            return timeslots;
        }
        duration = duration - (int) time;
        timeslot.setDuration((int) time);
        timeslots.add(timeslot);
        timeslots.addAll(others);
        while (duration > 0) {
            Timeslot newTimeslot = new Timeslot();
            BeanUtils.copyProperties(timeslot, newTimeslot);
            index++;
            newTimeslot.setId(timeslot.getProcedure().getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() + "_" + index);
            newTimeslot.setDuration(Math.min(duration, (int) time));
            newTimeslot.setIndex(index);
            timeslots.add(newTimeslot);
            duration = duration - (int) time;
        }
        int total = timeslots.size();
        return timeslots.stream().peek(t -> t.setTotal(total)).collect(Collectors.toList());
    }

    private List<Timeslot> splitTimeslot(Timeslot timeslot, List<Timeslot> others, int slice) {
        List<Timeslot> timeslots = new ArrayList<>();
        int duration = timeslot.getDuration();
        int index = timeslot.getTotal();
        int interval = Math.round((float) duration / slice * 100) / 100;
        timeslot.setDuration(interval);
        timeslots.add(timeslot);
        timeslots.addAll(others);
        for (int i = 1; i < slice; i++) {
            Timeslot newTimeslot = new Timeslot();
            BeanUtils.copyProperties(timeslot, newTimeslot);
            index++;
            newTimeslot.setId(timeslot.getProcedure().getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() + "_" + index);
            newTimeslot.setIndex(index);
            newTimeslot.setDuration(Math.min(duration - (interval * i), interval));
            timeslots.add(newTimeslot);
        }
        int total = timeslots.size();
        return timeslots.stream().peek(t -> t.setTotal(total)).collect(Collectors.toList());
    }


    public void splitOutsourcingTimeslot(String timeId, int days) {
        Timeslot timeslot = timeslotRepository.findById(timeId).orElse(null);
        if (timeslot == null) {
            return;
        }
        timeslot.setDuration(480);
        timeslotRepository.save(timeslot);
        Procedure procedure = timeslot.getProcedure();
        List<Timeslot> timeslots = timeslotRepository.findAllByProcedure(procedure);
        timeslot = timeslots.stream().max(Comparator.comparing(Timeslot::getIndex)).orElse(timeslot);
        timeslot.setDuration(480);
        timeslotRepository.save(timeslot);
        int index = timeslot.getIndex();
        for (int i = 1; i < days; i++) {
            Timeslot newTimeslot = new Timeslot();
            BeanUtils.copyProperties(timeslot, newTimeslot);
            index++;
            newTimeslot.setId(timeslot.getProcedure().getTask().getTaskNo() + "_" + timeslot.getProcedure().getProcedureNo() + "_" + index);
            newTimeslot.setIndex(index);
            timeslotRepository.save(newTimeslot);
        }
    }

}
