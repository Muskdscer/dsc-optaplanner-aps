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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MesOrderService {


    private MesJjOrderTaskService mesJjOrderTaskService;
    private MesJjProcedureService mesJjProcedureService;
    private OrderService orderService;
    private OrderTaskService orderTaskService;
    private ProcedureService procedureService;
    private WorkCenterService workCenterService;
    private TimeslotService timeslotService;

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

    public List<Timeslot> mergePlannerData(List<Order> orders) {
        List<MesJjOrderTask> mesOrderTasks = getOrderTasks(orders);
        List<MesJjProcedure> mesProcedures = getProcedures(mesOrderTasks);
        List<WorkCenter> workCenters = workCenterService.getAllMachines();
        List<Task> tasks = convertTasks(mesOrderTasks);
        Map<String, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getOrderNo, order -> order));
        Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getTaskNo, task -> task));
        List<Procedure> procedures = convertProcedures(
                mesProcedures.stream().distinct().collect(Collectors.toList()),
                workCenters,
                orderMap,
                taskMap);
        List<Timeslot> timeslots = new ArrayList<>();
        for (Procedure procedure : procedures) {
            timeslots.add(createTimeslot(procedure));
        }
        return timeslotService.saveTimeslot(timeslots);
    }

    private Timeslot createTimeslot(Procedure procedure) {
        Timeslot timeslot = new Timeslot();
        timeslot.setId(procedure.getTask().getTaskNo() + "_" + procedure.getProcedureNo() + "_" + 1);
        timeslot.setProcedure(procedure);
        timeslot.setStartTime(procedure.getStartTime());
        if (procedure.getTask() != null) {
            timeslot.setPriority(timeslot.getProcedure().getTask().getPriority());
        }
        timeslot.setIndex(1);
        timeslot.setTotal(1);
        timeslot.setProcedureIndex(procedure.getIndex());
        timeslot.setParallel(procedure.isParallel());
        timeslot.setDuration(procedure.getMachineMinutes());
        if (procedure.getStartTime() != null && procedure.getEndTime() != null) {
            timeslot.setManual(true);
            timeslot.setStartTime(procedure.getStartTime());
        }
        return timeslot;
    }


    private List<MesJjOrderTask> getOrderTasks(List<Order> orders) {
        List<MesJjOrderTask> orderTasks = new ArrayList<>();
        Lists.partition(orders, 999).forEach(taskNo -> {
            List<String> orderNos = taskNo.stream().filter(Objects::nonNull).map(Order::getOrderNo).collect(Collectors.toList());
            orderTasks.addAll(mesJjOrderTaskService.queryAllByOrderNoInAndTaskStatusIn(orderNos, List.of("生产中", "待生产")));
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

    private List<Task> convertTasks(List<MesJjOrderTask> mesOrderTasks) {
        List<Task> tasks = new ArrayList<>();
        for (MesJjOrderTask orderTask : mesOrderTasks) {
            Task task = new Task();
            task.setOrderNo(orderTask.getOrderNo());
            task.setTaskNo(orderTask.getTaskNo());
            task.setStatus(orderTask.getTaskStatus());
            task.setCreateDate(DateUtils.parseDateTime(orderTask.getCreateDate()));
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
            if (StringUtils.hasLength(orderTask.getPlanQuantity())) {
                task.setPlanQuantity(Integer.parseInt(orderTask.getPlanQuantity()));
            }
            if (StringUtils.hasLength(orderTask.getLockedRemark())) {
                task.setLockedRemark(orderTask.getLockedRemark());
            }
            if (StringUtils.hasLength(orderTask.getRouteSeq())) {
                task.setRouteId(orderTask.getRouteSeq());
            }
            tasks.add(task);
        }
        return orderTaskService.saveAll(tasks);
    }

    private List<Procedure> convertProcedures(List<MesJjProcedure> mesProcedures, List<WorkCenter> workCenters,
                                              Map<String, Order> orders, Map<String, Task> tasks) {
        Map<String, WorkCenter> workCenterMap = workCenters.stream().collect(Collectors.toMap(WorkCenter::getId, workCenter -> workCenter));
        List<Procedure> procedures = new ArrayList<>();
        for (MesJjProcedure mesProcedure : mesProcedures) {
            if (mesProcedure.getProcedureNo().equals("15")) {
                continue;
            }
            Procedure procedure = new Procedure();
            Integer procedureNo = Integer.parseInt(mesProcedure.getProcedureNo());
            procedure.setId(mesProcedure.getSeq());
            procedure.setProcedureName(mesProcedure.getProcedureName());
            procedure.setStatus(mesProcedure.getProcedureStatus());
            procedure.setProcedureNo(procedureNo);
            procedure.setWorkCenter(workCenterMap.get(mesProcedure.getWorkCenterSeq()));
            procedure.setOrder(orders.get(mesProcedure.getOrderNo()));
            procedure.setTask(tasks.get(mesProcedure.getTaskNo()));
            procedure.setProcedureType(mesProcedure.getProcedureType());
            procedure.setCreateDate(DateUtils.parseDateTime(mesProcedure.getCreatedate()));
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
            if (mesProcedure.getMachineHours() != null) {
                procedure.setMachineMinutes((int) (Double.parseDouble(mesProcedure.getMachineHours()) * 60));
            }
            if (StringUtils.hasLength(mesProcedure.getHumanHours())) {
                procedure.setHumanMinutes((int) (Double.parseDouble(mesProcedure.getHumanHours()) * 60));
            }
            if (StringUtils.hasLength(mesProcedure.getReworkFlag())) {
                procedure.setRework(mesProcedure.getReworkFlag().equals("1"));
            }
            procedures.add(procedure);
        }
        procedures = procedureService.saveProcedures(procedures);
        Map<String, Procedure> map = procedures.stream()
                .collect(Collectors.toMap(p -> p.getTask().getTaskNo() + "_" + p.getProcedureNo(), m1 -> m1, (p1, p2) -> p1));
        for (Procedure procedure : procedures) {
            List<Integer> numbers = procedure.getNextProcedureNo();
            if (CollectionUtil.isEmpty(numbers)) {
                continue;
            }
            for (Integer number : numbers) {
                Procedure nextProcedure = map.get(procedure.getTask().getTaskNo() + "_" + number);
                if (nextProcedure != null) {
                    if (numbers.size() >= 2) {
                        nextProcedure.setParallel(true);
                    }
                    procedure.addNextProcedure(nextProcedure);
                }
            }
        }
        Map<String, List<Procedure>> maps = procedures.stream().collect(Collectors.groupingBy(procedure -> procedure.getTask().getTaskNo()));
        for (List<Procedure> value : maps.values()) {
            Procedure procedure = value.stream().min(Comparator.comparing(Procedure::getProcedureNo)).orElse(null);
            NodeLevelManager.calculateLevels(procedure);

        }
        return procedureService.saveProcedures(procedures);
    }

    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("mySqlTemplate")
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void syncMesOrders() {
        LocalDateTime now = LocalDate.now().minusDays(1).atStartOfDay();
        String start = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sql = " select t1.ORDERNO, " +
                "       t1.PLAN_QUANTITY, " +
                "       t1.ORDER_STATUS, " +
                "       t1.ERP_STATUS, " +
                "       t1.CONTRACTNUM as CONTRACT_NUM, " +
                "       t1.PLAN_STARTDATE as PLAN_START_DATE, " +
                "       t1.PLAN_ENDDATE as PLAN_END_DATE,  " +
                "       t3.PRODUCT_CODE, " +
                "       t3.PRODUCT_NAME, " +
                "       t1.CREATEDATE as CREATE_DATE, " +
                "       t1.FACT_STARTDATE as FACT_START_DATE, " +
                "       t1.FACT_ENDDATE as FACT_END_DATE from MES_JJ_ORDER t1 " +
                " inner join MES_JJ_ORDER_PRODUCT_INFO t3 on t1.ORDERNO=t3.ORDERNO " +
                " where t1.ORDER_STATUS <> '生产完成' AND t1.ORDERNO like '00400%' " +
                " and t1.CREATEDATE >= '" + start + "' and t1.ORDERNO not in (select t2.ORDER_NO from APS_ORDERS t2 " +
                " where t2.CREATE_DATE >= TO_DATE('" + start + "','YYYY-MM-DD HH24:MI:SS'))";
        List<Order> orders = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Order.class));
        orderService.saveAll(orders);
        Lists.partition(orders, 100).forEach(this::mergePlannerData);
    }


    public void syncUpdateMesOrders() {
        String sql = " SELECT T1.* FROM MES_JJ_ORDER T1 " +
                " INNER JOIN APS_ORDERS T2 ON T2.ORDER_NO=T1.ORDERNO " +
                " WHERE T1.ORDER_STATUS!=T2.ORDER_STATUS " +
                "   AND (TO_NUMBER(T1.PLAN_QUANTITY)= T2.PLAN_QUANTITY " +
                "   OR TO_DATE(T1.PLAN_ENDDATE, 'YYYY-MM-DD') != T2.PLAN_END_DATE " +
                "   OR TO_DATE(T1.PLAN_STARTDATE, 'YYYY-MM-DD') != T2.PLAN_START_DATE " +
                "   OR TO_DATE(T1.FACT_STARTDATE, 'YYYY-MM-DD HH24:MI:SS')!= T2.FACT_START_DATE " +
                "   OR TO_DATE(T1.FACT_ENDDATE, 'YYYY-MM-DD HH24:MI:SS') != T2.FACT_END_DATE )" +
                " AND T1.CREATEDATE >= '2025-01-01 00:00:00' AND T1.ORDERNO LIKE '00400%' ";
        List<MesJjOrder> mesJjOrders = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(MesJjOrder.class));
        for (MesJjOrder mesJjOrder : mesJjOrders) {
            Order order = orderService.getOrderById(mesJjOrder.getOrderNo()).orElse(null);
            if (order != null) {
                order.setOrderStatus(mesJjOrder.getOrderStatus());
                order.setErpStatus(mesJjOrder.getErpStatus());
                order.setContractNum(mesJjOrder.getContractNum());
                order.setPlanQuantity(order.getPlanQuantity());
                order.setPlanStartDate(DateUtils.parseLocalDate(mesJjOrder.getPlanStartDate()));
                order.setPlanEndDate(DateUtils.parseLocalDate(mesJjOrder.getPlanEndDate()));
                order.setFactStartDate(DateUtils.parseDateTime(mesJjOrder.getFactStartDate()));
                order.setFactEndDate(DateUtils.parseDateTime(mesJjOrder.getFactEndDate()));
                order.setPlanQuantity(Integer.valueOf(mesJjOrder.getPlanQuantity()));
                orderService.save(order);
            }
        }
    }

    public void syncUpdateTask() {
        String sql = " SELECT T1.* FROM MES_JJ_ORDER_TASK T1 " +
                " INNER JOIN APS_TASK T2 ON T1.TASKNO = T2.TASK_NO " +
                " WHERE T1.TASK_STATUS != T2.STATUS " +
                "   AND ( TO_NUMBER(t1.PLAN_QUANTITY)= t2.PLANQUANTITY " +
                "   OR TO_DATE(T1.PLAN_ENDDATE, 'YYYY-MM-DD') != T2.PLAN_END_DATE " +
                "   OR TO_DATE(T1.PLAN_STARTDATE, 'YYYY-MM-DD') != T2.PLAN_START_DATE " +
                "   OR TO_DATE(T1.FACT_STARTDATE, 'YYYY-MM-DD HH24:MI:SS')!= T2.FACT_START_DATE " +
                "   OR TO_DATE(T1.FACT_ENDDATE, 'YYYY-MM-DD HH24:MI:SS') != T2.FACT_END_DATE )" +
                "  AND T1.CREATEDATE >= '2025-01-01 00:00:00' AND t1.ORDERNO like '00400%'  ";
        List<MesJjOrderTask> orderTasks = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(MesJjOrderTask.class));
        for (MesJjOrderTask orderTask : orderTasks) {
            Task task = orderTaskService.findById(orderTask.getTaskNo());
            if (task != null) {
                task.setStatus(orderTask.getTaskStatus());
                task.setRouteId(orderTask.getRouteSeq());
                task.setPlanStartDate(DateUtils.parseLocalDate(orderTask.getPlanStartDate()));
                task.setPlanEndDate(DateUtils.parseLocalDate(orderTask.getPlanEndDate()));
                task.setFactStartDate(DateUtils.parseDateTime(orderTask.getFactStartDate()));
                task.setFactEndDate(DateUtils.parseDateTime(orderTask.getFactEndDate()));
                task.setLockedRemark(orderTask.getLockedRemark());
                task.setPlanQuantity(Integer.valueOf(orderTask.getPlanQuantity()));
                orderTaskService.save(task);
            }
        }
    }

    public void syncUpdateProcedure() {

    }

}
