package com.upec.factoryscheduling.mes.service;

import com.google.common.collect.Lists;
import com.upec.factoryscheduling.aps.entity.*;
import com.upec.factoryscheduling.aps.service.*;
import com.upec.factoryscheduling.common.utils.DateUtils;
import com.upec.factoryscheduling.common.utils.NodeLevelManager;
import com.upec.factoryscheduling.common.utils.RandomFun;
import com.upec.factoryscheduling.mes.entity.*;
import com.upec.factoryscheduling.mes.repository.MesOrderRepository;
import com.xkzhangsan.time.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MesOrderService {


    private MesOrderRepository mesOrderRepository;
    private MesBaseWorkCenterService mesBaseWorkCenterService;
    private MesJjOrderTaskService mesJjOrderTaskService;
    private MesJjProcedureService mesJjProcedureService;
    private OrderService orderService;
    private OrderTaskService orderTaskService;
    private ProcedureService procedureService;
    private WorkCenterService workCenterService;
    private TimeslotService timeslotService;
    private ApsWorkCenterMaintenanceService apsWorkCenterMaintenanceService;
    private WorkCenterMaintenanceService workCenterMaintenanceService;
    private MesJjRouteProcedureService mesJjRouteProcedureService;

    @Autowired
    public void setMesOrderRepository(MesOrderRepository mesOrderRepository) {
        this.mesOrderRepository = mesOrderRepository;
    }

    @Autowired
    private void setMesBaseWorkCenterService(MesBaseWorkCenterService mesBaseWorkCenterService) {
        this.mesBaseWorkCenterService = mesBaseWorkCenterService;
    }

    @Autowired
    private void setMesJjOrderTaskService(MesJjOrderTaskService mesJjOrderTaskService) {
        this.mesJjOrderTaskService = mesJjOrderTaskService;
    }

    @Autowired
    private void setMesJjProcedureService(MesJjProcedureService mesJjProcedureService) {
        this.mesJjProcedureService = mesJjProcedureService;
    }

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Autowired
    public void setOrderTaskService(OrderTaskService orderTaskService) {
        this.orderTaskService = orderTaskService;
    }

    @Autowired
    public void setProcedureService(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    @Autowired
    public void setTimeslotService(TimeslotService timeslotService) {
        this.timeslotService = timeslotService;
    }

    @Autowired
    public void setWorkCenterService(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }


    @Autowired
    public void setApsWorkCenterMaintenanceService(ApsWorkCenterMaintenanceService apsWorkCenterMaintenanceService) {
        this.apsWorkCenterMaintenanceService = apsWorkCenterMaintenanceService;
    }

    @Autowired
    public void setWorkCenterMaintenanceService(WorkCenterMaintenanceService workCenterMaintenanceService) {
        this.workCenterMaintenanceService = workCenterMaintenanceService;
    }

    @Autowired
    public void setMesJjRouteProcedureService(MesJjRouteProcedureService mesJjRouteProcedureService) {
        this.mesJjRouteProcedureService = mesJjRouteProcedureService;
    }

    public List<Timeslot> mergePlannerData(List<String> taskNos) {

        List<MesJjOrderTask> mesOrderTasks = getOrderTasks(taskNos);
        List<MesJjOrder> mesOrders =
                getMesOrders(mesOrderTasks.stream().map(MesJjOrderTask::getOrderNo).distinct().collect(Collectors.toList()));
        List<MesJjProcedure> mesProcedures = getProcedures(mesOrderTasks);
        List<String> workCenterCodes =
                mesProcedures.stream().map(MesJjProcedure::getWorkCenterSeq).distinct().collect(Collectors.toList());
        List<MesBaseWorkCenter> mesBaseWorkCenters = mesBaseWorkCenterService.findByIdIn(workCenterCodes);
        List<ApsWorkCenterMaintenance> apsWorkCenterMaintenances =
                apsWorkCenterMaintenanceService.findAllByWorkCenterCodeIn(mesBaseWorkCenters.stream()
                        .map(MesBaseWorkCenter::getWorkCenterCode).distinct().collect(Collectors.toList()));
        List<WorkCenter> workCenters = convertWorkCenters(mesBaseWorkCenters);
        convertMaintenance(apsWorkCenterMaintenances, workCenters);
        List<Order> orders = convertOrders(mesOrders);
        List<Task> tasks = convertTasks(mesOrderTasks);
        List<Procedure> procedures = convertProcedures(mesProcedures.stream().distinct().collect(Collectors.toList()), workCenters);
        List<Timeslot> timeslots = new ArrayList<>();
        Map<String, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getOrderNo, order -> order));
        Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskNo, task -> task));
        for (Procedure procedure : procedures) {
            timeslots.add(createTimeslot(procedure, orderMap.get(procedure.getOrderNo()), taskMap.get(procedure.getTaskNo())));
        }
        return timeslotService.saveTimeslot(timeslots);
    }

    private Timeslot createTimeslot(Procedure procedure, Order order, Task task) {
        Timeslot timeslot = new Timeslot();
        timeslot.setId(procedure.getTaskNo() + "_" + procedure.getProcedureNo() + "_" + 1);
        timeslot.setProcedure(procedure);
        timeslot.setWorkCenter(procedure.getWorkCenterId());
        timeslot.setOrder(order);
        timeslot.setTask(task);
        timeslot.setStartTime(procedure.getStartTime());
        timeslot.setEndTime(procedure.getEndTime());
        if (task != null) {
            timeslot.setPriority(timeslot.getTask().getPriority());
        }
        timeslot.setIndex(1);
        timeslot.setTotal(1);
        timeslot.setProcedureIndex(procedure.getIndex());
        timeslot.setParallel(procedure.isParallel());
        timeslot.setDuration(procedure.getMachineMinutes());
        if (procedure.getStartTime() != null && procedure.getEndTime() != null) {
            timeslot.setManual(true);
            timeslot.setStartTime(procedure.getStartTime());
            timeslot.setEndTime(procedure.getEndTime());
        }
        return timeslot;
    }


    private List<MesJjOrderTask> getOrderTasks(List<String> taskNos) {
        List<MesJjOrderTask> orderTasks = new ArrayList<>();
        Lists.partition(taskNos, 999).forEach(taskNo -> {
            orderTasks.addAll(mesJjOrderTaskService.queryAllByTaskNoInAndTaskStatusIn(taskNo, List.of("生产中", "待生产")));
        });
        return orderTasks;
    }

    private List<MesJjProcedure> getProcedures(List<MesJjOrderTask> orderTasks) {
        List<String> taskNos =
                orderTasks.stream().map(MesJjOrderTask::getTaskNo).distinct().collect(Collectors.toList());
        List<MesJjProcedure> procedures = new ArrayList<>();
        Lists.partition(taskNos, 999).forEach(taskNo -> {
            procedures.addAll(mesJjProcedureService.findAllByTaskNo(taskNo));
        });
        return procedures;
    }

    private List<MesJjRouteProcedure> getRouteProcedures(List<MesJjProcedure> procedures) {
        List<String> routeSeqs = procedures.stream().map(MesJjProcedure::getRouteSeq).distinct().collect(Collectors.toList());
        List<MesJjRouteProcedure> routeProcedures = new ArrayList<>();
        Lists.partition(routeSeqs, 999).forEach(routeSeq -> {
            routeProcedures.addAll(mesJjRouteProcedureService.findAllByRouteSeqIn(routeSeq));
        });
        return routeProcedures;
    }


    private List<WorkCenterMaintenance> convertMaintenance(List<ApsWorkCenterMaintenance> apsWorkCenterMaintenances,
                                                           List<WorkCenter> workCenters) {
        Map<String, WorkCenter> workCenterMap =
                workCenters.stream().collect(Collectors.toMap(WorkCenter::getWorkCenterCode, workCenter -> workCenter));
        List<WorkCenterMaintenance> maintenances = new ArrayList<>();
        for (ApsWorkCenterMaintenance apsWorkCenterMaintenance : apsWorkCenterMaintenances) {
            WorkCenterMaintenance maintenance = new WorkCenterMaintenance();
            maintenance.setYear(DateUtils.parseLocalDate(apsWorkCenterMaintenance.getLocalDate()).getYear());
            maintenance.setCapacity(apsWorkCenterMaintenance.getCapacity());
            maintenance.setDate(DateUtils.parseLocalDate(apsWorkCenterMaintenance.getLocalDate()));
            maintenance.setWorkCenter(workCenterMap.get(apsWorkCenterMaintenance.getWorkCenterCode()));
            maintenance.setStartTime(DateUtils.parseDateTime(apsWorkCenterMaintenance.getStartTime()).toLocalTime());
            maintenance.setEndTime(DateUtils.parseDateTime(apsWorkCenterMaintenance.getEndTime()).toLocalTime());
            maintenance.setStatus(apsWorkCenterMaintenance.getStatus());
            maintenance.setId(RandomFun.getInstance().getRandom());
            maintenance.setDescription(apsWorkCenterMaintenance.getDescription());
            maintenances.add(maintenance);
        }
        return workCenterMaintenanceService.createMachineMaintenance(maintenances);
    }

    private List<Order> convertOrders(List<MesJjOrder> mesOrders) {
        List<Order> orders = new ArrayList<>();
        for (MesJjOrder mesOrder : mesOrders) {
            Order order = new Order();
            order.setOrderNo(mesOrder.getOrderNo());
            order.setOrderStatus(mesOrder.getOrderStatus());
            order.setErpStatus(mesOrder.getErpStatus());
            if (StringUtils.hasLength(mesOrder.getPlanStartDate())) {
                order.setPlanStartDate(DateUtils.parseLocalDate(mesOrder.getPlanStartDate()));
            }
            if (StringUtils.hasLength(mesOrder.getPlanEndDate())) {
                order.setPlanEndDate(DateUtils.parseLocalDate(mesOrder.getPlanEndDate()));
            }
            if (StringUtils.hasLength(mesOrder.getFactStartDate())) {
                order.setFactStartDate(DateUtils.parseDateTime(mesOrder.getFactStartDate()));
            }
            if (StringUtils.hasLength(mesOrder.getFactEndDate())) {
                order.setFactEndDate(DateUtils.parseDateTime(mesOrder.getFactEndDate()));
            }
            orders.add(order);
        }
        return orderService.saveAll(orders);
    }

    private List<Task> convertTasks(List<MesJjOrderTask> mesOrderTasks) {
        List<Task> tasks = new ArrayList<>();
        for (MesJjOrderTask orderTask : mesOrderTasks) {
            Task task = new Task();
            task.setOrderNo(orderTask.getOrderNo());
            task.setTaskNo(orderTask.getTaskNo());
            task.setStatus(orderTask.getTaskStatus());
            if (StringUtils.hasLength(orderTask.getPlanStartDate())) {
                task.setPlanStartDate(DateUtils.parseLocalDate(orderTask.getPlanStartDate()));
            }
            if (StringUtils.hasLength(orderTask.getPlanEndDate())) {
                task.setPlanEndDate(DateUtils.parseLocalDate(orderTask.getPlanEndDate()));
            }
            if (StringUtils.hasLength(orderTask.getFactStartDate())) {
                task.setFactStartDate(DateUtils.parseDateTime(orderTask.getFactStartDate()));
            }
            if (StringUtils.hasLength(orderTask.getFactEndDate())) {
                task.setFactEndDate(DateUtils.parseDateTime(orderTask.getFactEndDate()));
            }
            if (StringUtils.hasLength(orderTask.getMark())) {
                task.setPriority(100);
            }
            tasks.add(task);
        }
        return orderTaskService.saveAll(tasks);
    }

    private List<Procedure> convertProcedures(List<MesJjProcedure> mesProcedures, List<WorkCenter> workCenters) {
        Map<String, WorkCenter> workCenterMap =
                workCenters.stream().collect(Collectors.toMap(WorkCenter::getId, workCenter -> workCenter));
        List<MesJjRouteProcedure> routeProcedures = getRouteProcedures(mesProcedures);
        Map<String, MesJjRouteProcedure> routeProcedureMap = routeProcedures.stream()
                .collect(Collectors.toMap(m -> m.getRouteSeq() + "_" + m.getProcedureNo(), m -> m));
        List<Procedure> procedures = new ArrayList<>();

        for (MesJjProcedure mesProcedure : mesProcedures) {
            if (mesProcedure.getProcedureNo().equals("15")) {
                continue;
            }
            MesJjRouteProcedure routeProcedure =
                    routeProcedureMap.get(mesProcedure.getRouteSeq() + "_" + mesProcedure.getProcedureNo());
            Procedure procedure = new Procedure();
            Integer procedureNo = Integer.parseInt(mesProcedure.getProcedureNo());
            procedure.setId(mesProcedure.getSeq());
            procedure.setOrderNo(mesProcedure.getOrderNo());
            procedure.setTaskNo(mesProcedure.getTaskNo());
            procedure.setWorkCenterId(workCenterMap.get(mesProcedure.getWorkCenterSeq()));
            procedure.setProcedureName(mesProcedure.getProcedureName());
            procedure.setStatus(mesProcedure.getProcedureStatus());
            procedure.setProcedureNo(procedureNo);
            if (StringUtils.hasLength(mesProcedure.getNextProcedureNo())) {
                String[] nextProcedureNos = mesProcedure.getNextProcedureNo().split(",");
                List<Integer> numbers = new ArrayList<>();
                for (String nextProcedureNo : nextProcedureNos) {
                    numbers.add(Integer.parseInt(nextProcedureNo));
                }
                procedure.setNextProcedureNo(numbers);
            }
            if (StringUtils.hasLength(mesProcedure.getPlanStartDate())) {
                procedure.setPlanStartDate(DateUtils.parseLocalDate(mesProcedure.getPlanStartDate()));
            }
            if (StringUtils.hasLength(mesProcedure.getPlanEndDate())) {
                procedure.setPlanEndDate(DateUtils.parseLocalDate(mesProcedure.getPlanEndDate()));
            }
            if (StringUtils.hasLength(mesProcedure.getFactStartDate())) {
                procedure.setStartTime(DateUtils.parseDateTime(mesProcedure.getFactStartDate()));
            }
            if (StringUtils.hasLength(mesProcedure.getFactEndDate())) {
                procedure.setEndTime(DateUtils.parseDateTime(mesProcedure.getFactEndDate()));
            }
            if (routeProcedure != null && routeProcedure.getMachineHours() != null) {
                procedure.setMachineMinutes((int) (Double.parseDouble(routeProcedure.getMachineHours()) * 60));
            }
            procedures.add(procedure);
        }
        procedures = procedureService.saveProcedures(procedures);
        Map<String, Procedure> map = procedures.stream()
                .collect(Collectors.toMap(p -> p.getTaskNo() + "_" + p.getProcedureNo(), m1 -> m1, (p1, p2) -> p1));
        for (Procedure procedure : procedures) {
            List<Integer> numbers = procedure.getNextProcedureNo();
            if (CollectionUtil.isEmpty(numbers)) {
                continue;
            }
            for (Integer number : numbers) {
                Procedure nextProcedure = map.get(procedure.getTaskNo() + "_" + number);
                if (nextProcedure != null) {
                    if (numbers.size() >= 2) {
                        nextProcedure.setParallel(true);
                    }
                    procedure.addNextProcedure(nextProcedure);
                }
            }
        }
        Map<String, List<Procedure>> maps = procedures.stream().collect(Collectors.groupingBy(Procedure::getTaskNo));
        for (List<Procedure> value : maps.values()) {
            Procedure procedure = value.stream().min(Comparator.comparing(Procedure::getProcedureNo)).orElse(null);
            NodeLevelManager.calculateLevels(procedure);

        }
        return procedureService.saveProcedures(procedures);
    }

    private List<WorkCenter> convertWorkCenters(List<MesBaseWorkCenter> mesBaseWorkCenters) {
        List<WorkCenter> workCenters = new ArrayList<>();
        for (MesBaseWorkCenter baseWorkCenter : mesBaseWorkCenters) {
            WorkCenter workCenter = new WorkCenter();
            workCenter.setId(baseWorkCenter.getSeq());
            workCenter.setName(baseWorkCenter.getDescription());
            workCenter.setWorkCenterCode(baseWorkCenter.getWorkCenterCode());
            workCenter.setStatus(baseWorkCenter.getStatus());
            workCenters.add(workCenter);
        }
        return workCenterService.saveWorkCenters(workCenters);
    }

    public List<MesJjOrder> getMesOrders(List<String> orderNos) {
        if (CollectionUtil.isNotEmpty(orderNos)) {
            return mesOrderRepository.findAllByOrderNoIn(orderNos);
        }
        return mesOrderRepository.findAllByPlanStartDateAfterAndOrderStatusIn("2025-01-01", List.of("生产中","待生产"));
    }
}
