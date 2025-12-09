package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * 工厂调度约束提供器
 * <p>实现OptaPlanner的ConstraintProvider接口，定义了工厂调度问题中的所有约束条件：</p>
 * <ul>
 *   <li>硬约束（Hard Constraints）：必须满足的规则，违反会导致解决方案不可行</li>
 *   <li>软约束（Soft Constraints）：应当尽量满足的规则，违反会降低解决方案质量</li>
 * </ul>
 * <p>这些约束条件共同构成了工厂调度优化的评估体系，指导OptaPlanner找到最佳调度方案。</p>
 */
@Slf4j  // Lombok注解，提供日志记录功能
@Component  // Spring组件，使此类可被自动注入
public class FactorySchedulingConstraintProvider implements ConstraintProvider, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 定义所有调度约束条件
     * <p>返回约束数组，包含系统中所有的调度规则，分为以下几类：</p>
     * <ul>
     *   <li>核心约束：保证调度的基本可行性</li>
     *   <li>资源约束：处理设备维护等资源限制</li>
     *   <li>优化约束：提高调度方案的整体质量</li>
     *   <li>分片约束：处理工序分片执行的特殊规则</li>
     * </ul>
     *
     * @param constraintFactory OptaPlanner提供的约束工厂，用于构建约束条件
     * @return 约束数组，包含所有定义的调度规则
     */
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // 硬约束 - 必须满足
                workCenterMaintenanceAllocationConstraint(constraintFactory),
//                procedureSequenceConstraint(constraintFactory),
//                procedureSliceSequenceConstraint(constraintFactory),
//                workCenterConflict(constraintFactory),
//                workCenterMaintenanceConflict(constraintFactory),
//                fixedStartTimeConstraint(constraintFactory),
//                sameDayOrderProcedureMachineConflict(constraintFactory),
                // 软约束 - 优化目标
//                maximizeOrderPriority(constraintFactory),
//                maximizeMachineUtilization(constraintFactory),
//                minimizeMakespan(constraintFactory),
//                orderStartDateProximity(constraintFactory),
//                procedureSlicePreferContinuous(constraintFactory)
        };
    }

    /**
     * 工作中心维护分配约束 - 硬约束
     * 确保时间槽分配的工作中心维护计划与工作中心匹配
     */
    private Constraint workCenterMaintenanceAllocationConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot ->  timeslot.getWorkCenter() != null)
                .filter(this::workCenterProximity)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Work Center Maintenance Allocation Constraint");
    }


    private boolean workCenterProximity(Timeslot timeslot) {
        // 使用局部变量复制，避免多线程访问中的不一致性
        if (timeslot == null) {
            return false;
        }
        // 原子获取属性值，避免读取过程中值被其他线程修改
        String workCenterCode = timeslot.getWorkCenter() != null ? timeslot.getWorkCenter().getWorkCenterCode() : null;
        String maintenanceWorkCenterCode = (timeslot.getMaintenance() != null && timeslot.getMaintenance().getWorkCenter() != null) 
                ? timeslot.getMaintenance().getWorkCenter().getWorkCenterCode() 
                : null;
        // 使用BigDecimal兼容的类型处理
        Number maintenanceCapacityObj = timeslot.getMaintenance() != null ? timeslot.getMaintenance().getCapacity() : null;
        Number maintenanceUsageTimeObj = timeslot.getMaintenance() != null ? timeslot.getMaintenance().getUsageTime() : null;
        Integer maintenanceCapacity = maintenanceCapacityObj != null ? maintenanceCapacityObj.intValue() : null;
        Integer maintenanceUsageTime = maintenanceUsageTimeObj != null ? maintenanceUsageTimeObj.intValue() : null;
        
        return workCenterCode != null && maintenanceWorkCenterCode != null && !workCenterCode.equals(maintenanceWorkCenterCode) && maintenanceCapacity.compareTo(maintenanceUsageTime) >= 0;
    }

    /**
     * 工序顺序约束 - 硬约束
     * 确保有前后依赖关系的工序按正确顺序执行
     */
    private Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        // 确保两个时间槽都有有效的开始时间
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> timeslot1.getTask() != null && timeslot2.getTask() != null
                        && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                        && timeslot1.getTask().getTaskNo().equals(timeslot2.getTask().getTaskNo())
                        && timeslot1.getProcedureIndex() < timeslot2.getProcedureIndex()
                        && procedureStartDateProximity(timeslot1, timeslot2))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Procedure sequence constraint");
    }


    private boolean procedureStartDateProximity(Timeslot t1, Timeslot t2) {
        // 使用局部变量复制，避免多线程访问中的不一致性
        if (t1 == null || t2 == null) {
            return false;
        }
        // 原子获取属性值，避免读取过程中值被其他线程修改
        LocalDateTime startTime1 = t1.getStartTime();
        LocalDateTime startTime2 = t2.getStartTime();
        
        return startTime1 != null && startTime2 != null && startTime2.isAfter(startTime1);
    }

    /**
     * 工序分片顺序约束 - 硬约束
     * 确保同一工序的不同分片按索引顺序执行
     * 合并了timeslotIndexOrderConstraint和procedureSliceSequence的功能
     */
    private Constraint procedureSliceSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                        && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                        && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId())
                        && procedureStartDateProximity(timeslot1, timeslot2))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Procedure slice sequence constraint");
    }

    /**
     * 工作中心冲突约束 - 硬约束
     * 确保同一工作中心在同一时间只能执行一个工序
     * 优化：同一任务和同一工序的时间槽，同一天的Maintenance只能被相同的工序分配一次
     */
    private Constraint workCenterConflict(ConstraintFactory constraintFactory) {
        // 合并两个约束逻辑：基本工作中心冲突检查 + 同一天Maintenance限制
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> {
                    // 使用局部变量复制，避免多线程访问中的不一致性
                    if (timeslot1 == null || timeslot2 == null) {
                        return false;
                    }
                    
                    // 原子获取属性值，处理ID类型转换
                    String timeslot1IdStr = timeslot1.getId() != null ? timeslot1.getId().toString() : null;
                    String timeslot2IdStr = timeslot2.getId() != null ? timeslot2.getId().toString() : null;
                    Long timeslot1Id = timeslot1IdStr != null ? Long.valueOf(timeslot1IdStr) : null;
                    Long timeslot2Id = timeslot2IdStr != null ? Long.valueOf(timeslot2IdStr) : null;
                    
                    // 快速排除自身比较
                    if (timeslot1Id != null && timeslot2Id != null && timeslot1Id.equals(timeslot2Id)) {
                        return false;
                    }
                    
                    // 获取工作中心和时间信息
                    String workCenterId1 = timeslot1.getWorkCenter() != null ? timeslot1.getWorkCenter().getId() : null;
                    String workCenterId2 = timeslot2.getWorkCenter() != null ? timeslot2.getWorkCenter().getId() : null;
                    LocalDateTime startTime1 = timeslot1.getStartTime();
                    LocalDateTime startTime2 = timeslot2.getStartTime();
                    
                    // 确保有有效信息才能继续检查
                    if (workCenterId1 == null || workCenterId2 == null || startTime1 == null || startTime2 == null) {
                        return false;
                    }
                    
                    // 检查基本工作中心冲突（同一工作中心且时间重叠）
                    boolean sameWorkCenter = workCenterId1.equals(workCenterId2);
                    
                    // 计算结束时间，使用局部变量避免多次访问
                    // 由于double不能为null，直接获取值，如果是Double类型则需要额外处理
                    double duration1 = timeslot1.getDuration();
                    double duration2 = timeslot2.getDuration();
                    LocalDateTime end1 = timeslot1.getEndTime() != null ? timeslot1.getEndTime() : startTime1.plusMinutes((long) (duration1 * 60));
                    LocalDateTime end2 = timeslot2.getEndTime() != null ? timeslot2.getEndTime() : startTime2.plusMinutes((long) (duration2 * 60));
                    
                    boolean timeOverlap = !(startTime1.isAfter(end2) || startTime2.isAfter(end1));
                    boolean basicConflict = sameWorkCenter && timeOverlap;
                    
                    // 如果已经确定有基本冲突，可以立即返回true
                    if (basicConflict) {
                        return true;
                    }

                    // 检查同一天Maintenance冲突（同一任务、同一工序，使用了同一天的Maintenance）
                    String taskNo1 = timeslot1.getTask() != null ? timeslot1.getTask().getTaskNo() : null;
                    String taskNo2 = timeslot2.getTask() != null ? timeslot2.getTask().getTaskNo() : null;
                    String procedureId1 = timeslot1.getProcedure() != null ? timeslot1.getProcedure().getId() : null;
                    String procedureId2 = timeslot2.getProcedure() != null ? timeslot2.getProcedure().getId() : null;
                    LocalDate maintenanceDate1 = timeslot1.getMaintenance() != null ? timeslot1.getMaintenance().getDate() : null;
                    LocalDate maintenanceDate2 = timeslot2.getMaintenance() != null ? timeslot2.getMaintenance().getDate() : null;
                    
                    boolean sameTask = taskNo1 != null && taskNo2 != null && taskNo1.equals(taskNo2);
                    boolean sameProcedure = procedureId1 != null && procedureId2 != null && procedureId1.equals(procedureId2);
                    boolean sameDayMaintenance = maintenanceDate1 != null && maintenanceDate2 != null && maintenanceDate1.equals(maintenanceDate2);
                    boolean maintenanceConflict = sameTask && sameProcedure && sameDayMaintenance;

                    // 只要违反任一约束，就返回true
                    return maintenanceConflict;
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                    // 使用局部变量复制，避免多线程访问中的不一致性
                    if (timeslot1 == null || timeslot2 == null) {
                        return 0;
                    }
                    
                    // 获取工作中心和时间信息
                    String workCenterId1 = timeslot1.getWorkCenter() != null ? timeslot1.getWorkCenter().getId() : null;
                    String workCenterId2 = timeslot2.getWorkCenter() != null ? timeslot2.getWorkCenter().getId() : null;
                    LocalDateTime startTime1 = timeslot1.getStartTime();
                    LocalDateTime startTime2 = timeslot2.getStartTime();
                    
                    // 确保有有效信息才能继续检查
                    if (workCenterId1 == null || workCenterId2 == null || startTime1 == null || startTime2 == null) {
                        return 0;
                    }

                    // 计算基本工作中心冲突的惩罚值
                    boolean sameWorkCenter = workCenterId1.equals(workCenterId2);
                    
                    // 计算结束时间，使用局部变量避免多次访问
                    // 由于double不能为null，直接获取值
                    double duration1 = timeslot1.getDuration();
                    double duration2 = timeslot2.getDuration();
                    LocalDateTime end1 = timeslot1.getEndTime() != null ? timeslot1.getEndTime() : startTime1.plusMinutes((long) (duration1 * 60));
                    LocalDateTime end2 = timeslot2.getEndTime() != null ? timeslot2.getEndTime() : startTime2.plusMinutes((long) (duration2 * 60));
                    
                    boolean timeOverlap = !(startTime1.isAfter(end2) || startTime2.isAfter(end1));
                    int basicPenalty = 0;
                    if (sameWorkCenter && timeOverlap) {
                        LocalDateTime overlapStart = startTime1.isAfter(startTime2) ? startTime1 : startTime2;
                        LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;
                        basicPenalty = (int) ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                    }

                    // 计算同一天Maintenance冲突的惩罚值
                    String taskNo1 = timeslot1.getTask() != null ? timeslot1.getTask().getTaskNo() : null;
                    String taskNo2 = timeslot2.getTask() != null ? timeslot2.getTask().getTaskNo() : null;
                    String procedureId1 = timeslot1.getProcedure() != null ? timeslot1.getProcedure().getId() : null;
                    String procedureId2 = timeslot2.getProcedure() != null ? timeslot2.getProcedure().getId() : null;
                    LocalDate maintenanceDate1 = timeslot1.getMaintenance() != null ? timeslot1.getMaintenance().getDate() : null;
                    LocalDate maintenanceDate2 = timeslot2.getMaintenance() != null ? timeslot2.getMaintenance().getDate() : null;
                    
                    boolean sameTask = taskNo1 != null && taskNo2 != null && taskNo1.equals(taskNo2);
                    boolean sameProcedure = procedureId1 != null && procedureId2 != null && procedureId1.equals(procedureId2);
                    boolean sameDayMaintenance = maintenanceDate1 != null && maintenanceDate2 != null && maintenanceDate1.equals(maintenanceDate2);
                    int maintenancePenalty = 0;
                    if (sameTask && sameProcedure && sameDayMaintenance) {
                        maintenancePenalty = 100; // 固定惩罚值
                    }

                    // 返回较大的惩罚值
                    return Math.max(basicPenalty, maintenancePenalty);
                })
                .asConstraint("Work center conflict");
    }

    /**
     * 工作中心维护冲突约束 - 硬约束
     * 确保工序不安排在工作中心维护期间
     */
    private Constraint workCenterMaintenanceConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getStartTime() != null)
                .join(WorkCenterMaintenance.class)
                .filter((timeslot, maintenance) -> {
                    // 检查工作中心是否相同
                    if (maintenance.getWorkCenter() == null || !maintenance.getWorkCenter().getId().equals(timeslot.getWorkCenter().getId())) {
                        return false;
                    }
                    // 检查维护状态是否为不可用
                    if (maintenance.getStatus() == null || !maintenance.getStatus().equals("n")) {
                        return false;
                    }
                    // 检查维护日期是否有效
                    if (maintenance.getDate() == null) {
                        return false;
                    }

                    // 检查时间是否重叠
                    LocalDateTime taskStart = timeslot.getStartTime();
                    LocalDateTime taskEnd = timeslot.getEndTime();
                    if (taskEnd == null) {
                        taskEnd = taskStart.plusMinutes((long) (timeslot.getDuration() * 60));
                    }
                    LocalDateTime maintenanceStart = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getStartTime() != null ? maintenance.getStartTime() : LocalTime.MIN);
                    LocalDateTime maintenanceEnd = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getEndTime() != null ? maintenance.getEndTime() : LocalTime.MAX);

                    // 检查时间重叠
                    return !(taskEnd.isBefore(maintenanceStart) || taskStart.isAfter(maintenanceEnd));
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot, maintenance) -> {
                    // 计算冲突时间
                    LocalDateTime taskStart = timeslot.getStartTime();
                    LocalDateTime taskEnd = timeslot.getEndTime();
                    if (taskEnd == null) {
                        taskEnd = taskStart.plusMinutes((timeslot.getDuration() * 60L));
                    }
                    LocalDateTime maintenanceStart = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getStartTime() != null ? maintenance.getStartTime() : LocalTime.MIN);
                    LocalDateTime maintenanceEnd = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getEndTime() != null ? maintenance.getEndTime() : LocalTime.MAX);

                    LocalDateTime overlapStart = taskStart.isAfter(maintenanceStart) ? taskStart : maintenanceStart;
                    LocalDateTime overlapEnd = taskEnd.isBefore(maintenanceEnd) ? taskEnd : maintenanceEnd;

                    if (overlapStart.isBefore(overlapEnd)) {
                        return (int) ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                    }
                    return 0;
                })
                .asConstraint("Work center maintenance conflict");
    }

    /**
     * 固定开始时间约束 - 硬约束
     * 确保有固定开始时间的工序按计划执行
     */
    private Constraint fixedStartTimeConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null && (
                        // 检查订单是否有固定开始时间
                        (timeslot.getOrder() != null && timeslot.getOrder().getFactStartDate() != null) ||
                                // 检查工序是否有固定开始时间或已完成
                                (timeslot.getProcedure() != null && (
                                        timeslot.getProcedure().getStartTime() != null ||
                                                timeslot.getProcedure().getEndTime() != null
                                ))
                ))
                .penalize(HardSoftScore.ONE_HARD, timeslot -> {
                    int penalty = 0;

                    // 检查订单固定开始时间
                    if (timeslot.getOrder() != null && timeslot.getOrder().getFactStartDate() != null) {
                        LocalDateTime factStartDate = timeslot.getOrder().getFactStartDate();
                        LocalDateTime plannedStartTime = timeslot.getStartTime();
                        if (!factStartDate.equals(plannedStartTime)) {
                            // 计算时间偏差（分钟）作为惩罚
                            long deviationMinutes = Math.abs(ChronoUnit.MINUTES.between(factStartDate, plannedStartTime));
                            penalty += (int) deviationMinutes;
                        }
                    }

                    // 检查工序固定开始时间
                    if (timeslot.getProcedure() != null && timeslot.getProcedure().getStartTime() != null) {
                        LocalDateTime procedureStartTime = timeslot.getProcedure().getStartTime();
                        LocalDateTime plannedStartTime = timeslot.getStartTime();
                        if (!procedureStartTime.equals(plannedStartTime)) {
                            // 计算时间偏差（分钟）作为惩罚
                            long deviationMinutes = Math.abs(ChronoUnit.MINUTES.between(procedureStartTime, plannedStartTime));
                            penalty += (int) deviationMinutes;
                        }
                    }

                    // 检查工序是否已完成（不应再被规划）
                    if (timeslot.getProcedure() != null && timeslot.getProcedure().getEndTime() != null) {
                        penalty += 100; // 已完成的工序不应再被规划，给予较高惩罚
                    }

                    return penalty;
                })
                .asConstraint("Fixed start time constraint");
    }

    /**
     * 同天同订单同工序同机器约束 - 硬约束
     * 确保同天同订单同工序同机器不能同时被安排两次
     */
    private Constraint sameDayOrderProcedureMachineConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> {
                    // 使用局部变量避免多次访问对象属性，确保线程安全
                    if (timeslot1 == null || timeslot2 == null) {
                        return false;
                    }
                    
                    // 原子获取属性值，避免多线程访问中的不一致性
                    Order order1 = timeslot1.getOrder();
                    Order order2 = timeslot2.getOrder();
                    Procedure procedure1 = timeslot1.getProcedure();
                    Procedure procedure2 = timeslot2.getProcedure();
                    WorkCenter workCenter1 = timeslot1.getWorkCenter();
                    WorkCenter workCenter2 = timeslot2.getWorkCenter();
                    LocalDateTime startTime1 = timeslot1.getStartTime();
                    LocalDateTime startTime2 = timeslot2.getStartTime();
                    
                    // 快速排除空值情况
                    if (order1 == null || order2 == null || procedure1 == null || procedure2 == null ||
                            workCenter1 == null || workCenter2 == null || startTime1 == null || startTime2 == null) {
                        return false;
                    }
                    
                    // 获取具体的ID值
                    String orderNo1 = order1.getOrderNo();
                    String orderNo2 = order2.getOrderNo();
                    String procedureId1 = procedure1.getId();
                    String procedureId2 = procedure2.getId();
                    String workCenterId1 = workCenter1.getId();
                    String workCenterId2 = workCenter2.getId();
                    
                    // 快速检查是否相同
                    boolean sameOrder = orderNo1 != null && orderNo2 != null && orderNo1.equals(orderNo2);
                    boolean sameProcedure = procedureId1 != null && procedureId2 != null && procedureId1.equals(procedureId2);
                    boolean sameMachine = workCenterId1 != null && workCenterId2 != null && workCenterId1.equals(workCenterId2);
                    boolean sameDay = startTime1.toLocalDate().equals(startTime2.toLocalDate());

                    // 如果满足所有条件，检查是否有时间重叠
                    if (sameOrder && sameProcedure && sameMachine && sameDay) {
                        // 使用局部变量计算结束时间，避免多次访问
                        // 由于double不能为null，直接获取值
                        double duration1 = timeslot1.getDuration();
                        double duration2 = timeslot2.getDuration();
                        LocalDateTime end1 = timeslot1.getEndTime() != null ? timeslot1.getEndTime() : startTime1.plusMinutes((long) (duration1 * 60));
                        LocalDateTime end2 = timeslot2.getEndTime() != null ? timeslot2.getEndTime() : startTime2.plusMinutes((long) (duration2 * 60));
                        boolean timeOverlap = !(startTime1.isAfter(end2) || startTime2.isAfter(end1));
                        return timeOverlap;
                    }

                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                    // 使用局部变量避免多次访问对象属性
                    if (timeslot1 == null || timeslot2 == null) {
                        return 1; // 保持原逻辑，硬约束固定惩罚
                    }
                    
                    // 对于硬约束，只要有重叠就给予固定惩罚
                    return 1;
                })
                .asConstraint("Same day, same order, same procedure, same machine conflict");
    }


    /**
     * 最大化订单优先级 - 软约束
     * 优先处理高优先级订单
     */
    private Constraint maximizeOrderPriority(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null && timeslot.getOrder() != null && timeslot.getPriority() != null)
                .reward(HardSoftScore.ONE_SOFT.multiply(10),
                        timeslot -> {
                            Integer priority = timeslot.getPriority();
                            // 优先级1（最高）给予最高奖励，优先级越高奖励越多
                            return Math.max(10, 110 - priority * 10);
                        })
                .asConstraint("Maximize order priority");
    }

    /**
     * 最大化机器利用率 - 软约束
     * 充分利用机器设备
     */
    private Constraint maximizeMachineUtilization(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getStartTime() != null)
                .join(WorkCenterMaintenance.class)
                .filter((timeslot, maintenance) -> {
                    // 确保工作中心可用且有容量信息
                    return maintenance.getWorkCenter().getId().equals(timeslot.getWorkCenter().getId())
                            && maintenance.getStatus() != null && maintenance.getStatus().equals("y")
                            && maintenance.getCapacity() > 0;
                })
                .groupBy((timeslot, maintenance) -> new WorkCenterUtilizationKey(
                        timeslot.getWorkCenter().getId(),
                        timeslot.getStartTime().toLocalDate(),
                        maintenance.getCapacity()
                ), ConstraintCollectors.sum((timeslot, maintenance) ->
                        (int) (timeslot.getDuration() * 60) // 转换为分钟
                ))
                .reward(HardSoftScore.ONE_SOFT,
                        (key, totalUsageMinutes) -> {
                            // 计算利用率百分比，最高100%
                            double capacityMinutes = key.getCapacity() * 60;
                            double utilization = ((double) totalUsageMinutes / capacityMinutes) * 100;
                            utilization = Math.min(utilization, 100); // 上限100%
                            return (int) utilization;
                        })
                .asConstraint("Maximize machine utilization");
    }

    // 辅助类，用于分组工作中心利用率数据
    @Getter
    private static class WorkCenterUtilizationKey {
        private final String workCenterId;
        private final LocalDate date;
        private final double capacity;

        public WorkCenterUtilizationKey(String workCenterId, LocalDate date, double capacity) {
            this.workCenterId = workCenterId;
            this.date = date;
            this.capacity = capacity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WorkCenterUtilizationKey that = (WorkCenterUtilizationKey) o;
            return workCenterId.equals(that.workCenterId) && date.equals(that.date);
        }

        @Override
        public int hashCode() {
            int result = workCenterId.hashCode();
            result = 31 * result + date.hashCode();
            return result;
        }
    }

    /**
     * 最小化制造周期 - 软约束
     * 尽量缩短整个生产周期
     */
    private Constraint minimizeMakespan(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                // 使用局部变量避免多次访问对象属性
                .filter(timeslot -> {
                    if (timeslot == null) {
                        return false;
                    }
                    return timeslot.getStartTime() != null;
                })
                // 惩罚每个任务的开始时间，鼓励尽早开始
                .penalize(HardSoftScore.ONE_SOFT,
                        timeslot -> {
                            // 使用局部变量副本，避免多线程访问中的不一致性
                            if (timeslot == null) {
                                return 0;
                            }
                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime taskStartTime = timeslot.getStartTime();
                            // 防御性检查
                            if (taskStartTime == null) {
                                return 0;
                            }
                            // 计算从当前时间到任务开始时间的分钟数
                            long delayMinutes = ChronoUnit.MINUTES.between(now, taskStartTime);
                            // 如果任务已经开始或为过去时间，不给予惩罚
                            if (delayMinutes <= 0) {
                                return 0;
                            }
                            // 每延迟30分钟增加1点惩罚
                            return (int) (delayMinutes / 30);
                        })
                .asConstraint("Minimize makespan");
    }

    /**
     * 订单开始时间接近度 - 软约束
     * 尽量让订单按计划开始时间执行
     */
    private Constraint orderStartDateProximity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null && timeslot.getOrder() != null
                        && timeslot.getOrder().getPlanStartDate() != null)
                .penalize(HardSoftScore.ONE_SOFT, timeslot -> {
                    LocalDate planStartDate = timeslot.getOrder().getPlanStartDate();
                    LocalDateTime actualStartTime = timeslot.getStartTime();
                    LocalDateTime planStartDateTime = planStartDate.atStartOfDay();
                    // 计算与计划开始时间的偏差（分钟）
                    long deviationMinutes = Math.abs(ChronoUnit.MINUTES.between(actualStartTime, planStartDateTime));
                    // 每偏离一天增加1点惩罚
                    return (int) (deviationMinutes / (24 * 60));
                })
                .asConstraint("Order start date proximity");
    }

    /**
     * 工序分片连续性 - 软约束
     * 尽量让同一工序的分片连续执行
     */
    private Constraint procedureSlicePreferContinuous(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        // 按工序ID分组
                        Joiners.equal(timeslot -> timeslot.getProcedure() != null ? timeslot.getProcedure().getId() : null),
                        // 按ID排序，确保只检查一次
                        Joiners.lessThan(Timeslot::getId))
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽属于同一工序且有有效的开始时间和索引
                    return timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                            && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                            && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId())
                            && timeslot2.getIndex() == timeslot1.getIndex() + 1;
                })
                .penalize(HardSoftScore.ONE_SOFT, (timeslot1, timeslot2) -> {
                    // 计算timeslot1的结束时间
                    LocalDateTime endTime1 = timeslot1.getEndTime();
                    if (endTime1 == null) {
                        // 使用持续时间计算结束时间，避免使用固定一天的默认值
                        int duration = timeslot1.getDuration();
                        endTime1 = timeslot1.getStartTime().plusMinutes(duration );
                    }

                    // 只有当endTime1在startTime2之前时才计算间隔
                    if (endTime1.isBefore(timeslot2.getStartTime())) {
                        // 计算时间间隔（分钟），间隔越大惩罚越大
                        long intervalMinutes = ChronoUnit.MINUTES.between(endTime1, timeslot2.getStartTime());
                        // 每小时间隔增加1点惩罚
                        return (int) (intervalMinutes / 60);
                    } else {
                        return 0;
                    }
                  })
                  .asConstraint("Procedure slice prefer continuous");
    }
}
