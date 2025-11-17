package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.*;
import com.upec.factoryscheduling.aps.repository.TimeslotRepository;
import com.upec.factoryscheduling.aps.resquest.ProcedureRequest;
import com.upec.factoryscheduling.aps.resquest.TimeslotRequest;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import io.micrometer.core.instrument.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.task.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeslotService {

    private WorkCenterMaintenanceService maintenanceService;
    private TimeslotRepository timeslotRepository;

    @Autowired
    public TimeslotService(WorkCenterMaintenanceService maintenanceService,
                           TimeslotRepository timeslotRepository) {
        this.maintenanceService = maintenanceService;
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
        return timeslotRepository.findAllByOrderIn(orders);
    }


    public void createTimeslot(List<String> taskNos, List<String> procedureIds, int time, int slice) {
        List<Procedure> procedures = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        List<Order> orders = new ArrayList<>();
        List<String> orderNos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(procedureIds)) {
            procedures = procedureService.findAllByIdIsIn(procedureIds);
        }
        if (!CollectionUtils.isEmpty(taskNos)) {
            procedures = procedureService.findAllByTaskNoIsIn(taskNos);
        }
        taskNos = procedures.stream().map(Procedure::getTaskNo).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(taskNos)) {
            tasks = orderTaskService.findAllByTaskNoIsIn(taskNos);
            orderNos = tasks.stream().map(Task::getOrderNo).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(orderNos)) {
            orders = orderService.queryByOrderNoIn(orderNos);
        }
        Map<String, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getOrderNo, m -> m));
        Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskNo, m -> m));
        List<Timeslot> timeslots = new ArrayList<>();
        // 为每个工序创建分片Timeslot
        for (Procedure procedure : procedures) {
            Order order = orderMap.get(procedure.getOrderNo());
            Task task = taskMap.get(procedure.getTaskNo());
            double duration = 0d;
            // 跳过没有绑定工作中心的工序
            if (procedure.getWorkCenterId() == null) {
                log.info("跳过未绑定工作中心的工序: {}", procedure.getId());
                continue;
            }
            // 检查是否有固定的开始时间和结束时间
            if (procedure.getStartTime() != null && procedure.getEndTime() != null) {
                // 情况1：工序有固定的开始时间和结束时间
                // 只创建一个分片
                Timeslot timeslot = createSingleTimeslot(procedure, task, order, procedure.getMachineHours(), 0, 1);
                timeslot.setStartTime(procedure.getStartTime()); // 设置固定的开始时间
                timeslot.setEndTime(procedure.getEndTime()); // 设置固定的结束时间
                timeslot.setManual(true); // 标记为手动设置，不需要参与规划
                timeslots.add(timeslot);
            }
            if (procedure.getStartTime() != null && procedure.getEndTime() == null) {
                // 情况2：工序只有固定的开始时间
                // 对于machineHours=0的工序，特殊处理
                if (procedure.getMachineHours() < 1.0) {
                    Timeslot timeslot = createSingleTimeslot(procedure, task, order, procedure.getMachineHours(), 0, 1);
                    timeslot.setDuration(procedure.getMachineHours());
                    timeslot.setStartTime(procedure.getStartTime());
                    timeslot.setEndTime(procedure.getStartTime()); // 开始时间和结束时间相同
                    timeslot.setManual(true);
                    timeslots.add(timeslot);
                }
                if (procedure.getMachineHours() >= 1.0) {
                    // 计算工序持续时间（小时）
                    duration = calculateProcedureDuration(procedure);
                    int total = 0;
                    if (slice > 0) {

                    }
                    if (duration <= 1.0) {
                        // 持续时间小于等于1小时的工序不需要分片
                        Timeslot timeslot = createSingleTimeslot(procedure, task, order, duration, 0, 1);
                        timeslot.setStartTime(procedure.getStartTime()); // 设置固定的开始时间
                        timeslot.setManual(true); // 标记为手动设置，开始时间必须以此为准
                        timeslots.add(timeslot);
                    } else {
                        // 持续时间大于1小时的工序需要分片
                        int totalSlices = (int) Math.ceil(duration); // 按小时分片
                        for (int i = 0; i < totalSlices; i++) {
                            Timeslot timeslot = createSingleTimeslot(procedure, task, order, duration, i, totalSlices);
                            // 设置当前分片的持续时间，最后一个分片可能不足1小时
                            timeslot.setDuration(i == totalSlices - 1 ? duration - i : 1.0);
                            // 只有第一个分片需要设置固定的开始时间
                            if (i == 0) {
                                timeslot.setStartTime(procedure.getStartTime()); // 设置固定的开始时间
                                timeslot.setManual(true); // 标记为手动设置
                            }
                            timeslots.add(timeslot);
                        }
                    }
                }
            } else {
                // 情况3：工序没有固定时间，按原逻辑处理
                // 对于machineHours=0的工序，特殊处理
                if (procedure.getMachineHours() == 0) {
                    Timeslot timeslot = createSingleTimeslot(procedure, task, order, duration, 0, 1);
                    timeslot.setDuration(0.0);
                    timeslot.setManual(true);
                    timeslots.add(timeslot);
                } else {
                    // 计算工序持续时间（小时）
                    duration = calculateProcedureDuration(procedure);
                    if (duration <= 1.0) {
                        // 持续时间小于等于1小时的工序不需要分片
                        Timeslot timeslot = createSingleTimeslot(procedure, task, order, duration, 0, 1);
                        timeslots.add(timeslot);
                    } else {
                        // 持续时间大于1小时的工序需要分片
                        int totalSlices = (int) Math.ceil(duration); // 按小时分片
                        for (int i = 0; i < totalSlices; i++) {
                            Timeslot timeslot = createSingleTimeslot(procedure, task, order, duration, i, totalSlices);
                            // 设置当前分片的持续时间，最后一个分片可能不足1小时
                            timeslot.setDuration(i == totalSlices - 1 ? duration - i : 1.0);
                            timeslots.add(timeslot);
                        }
                    }
                }
            }
        }
        saveTimeslot(timeslots);
    }


    /**
     * 创建单个时间槽
     */
    private Timeslot createSingleTimeslot(Procedure procedure,
                                          Task task,
                                          Order order,
                                          double duration,
                                          int sliceIndex,
                                          int totalSlices) {
        Timeslot timeslot = new Timeslot();
        // 使用工序ID加分片索引作为时间槽ID
        timeslot.setId(procedure.getTaskNo() + "_" + procedure.getProcedureNo() + "_" + sliceIndex);
        timeslot.setProcedure(procedure);
        timeslot.setWorkCenter(procedure.getWorkCenterId());
        timeslot.setOrder(order);
        timeslot.setTask(task);
        if (task != null) {
            timeslot.setPriority(timeslot.getTask().getPriority());
        }
        timeslot.setIndex(sliceIndex);
        timeslot.setTotal(totalSlices);
        timeslot.setDuration(duration);
        return timeslot;
    }

    /**
     * 计算工序持续时间（小时）
     */
    private double calculateProcedureDuration(Procedure procedure) {
        // 优先使用machineHours字段的值
        if (procedure.getMachineHours() > 0) {
            return procedure.getMachineHours();
        }
        // 如果没有machineHours，使用默认值1小时
        return 1.0;
    }


}
