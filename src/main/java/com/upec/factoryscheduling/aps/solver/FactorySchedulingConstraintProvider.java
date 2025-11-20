package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class FactorySchedulingConstraintProvider implements ConstraintProvider {
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
                fixedStartTimeConstraint(constraintFactory),
                workCenterConflict(constraintFactory),
                procedureSequenceConstraint(constraintFactory),
                workCenterMaintenanceConflict(constraintFactory),
                
                // 软约束 - 尽量满足
                orderStartDateProximity(constraintFactory),
                maximizeOrderPriority(constraintFactory),
                maximizeMachineUtilization(constraintFactory),
                minimizeMakespan(constraintFactory),
                procedureSliceSequence(constraintFactory)
        };
    }
    
    /**
     * 固定开始时间约束（硬约束）
     * <p>确保：</p>
     * <ul>
     *   <li>当Order的factStartDate不为空时，必须作为规划开始时间</li>
     *   <li>当Procedure的startTime不为空时，必须作为工序规划的开始时间</li>
     *   <li>当Procedure的endTime不为空时，表明工序已完成，不需要继续规划</li>
     * </ul>
     */
    private Constraint fixedStartTimeConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> {
                    // 检查订单是否有固定开始时间
                    if (timeslot.getOrder() != null && timeslot.getOrder().getFactStartDate() != null) {
                        return true;
                    }
                    // 检查工序是否有固定开始时间或已完成
                    if (timeslot.getProcedure() != null) {
                        return timeslot.getProcedure().getStartTime() != null 
                                || timeslot.getProcedure().getEndTime() != null;
                    }
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD, timeslot -> {
                            int penalty = 0;
                            
                            // 检查订单固定开始时间
                            if (timeslot.getOrder() != null && timeslot.getOrder().getFactStartDate() != null 
                                    && timeslot.getStartTime() != null) {
                                LocalDateTime factStartDate = timeslot.getOrder().getFactStartDate();
                                LocalDateTime plannedStartTime = timeslot.getStartTime();
                                if (!factStartDate.equals(plannedStartTime)) {
                                    penalty += 1;
                                }
                            }
                            
                            // 检查工序固定开始时间
                            if (timeslot.getProcedure() != null && timeslot.getProcedure().getStartTime() != null 
                                    && timeslot.getStartTime() != null) {
                                LocalDateTime procedureStartTime = timeslot.getProcedure().getStartTime();
                                LocalDateTime plannedStartTime = timeslot.getStartTime();
                                if (!procedureStartTime.equals(plannedStartTime)) {
                                    penalty += 1;
                                }
                            }
                            
                            // 检查工序是否已完成（不应再被规划）
                            if (timeslot.getProcedure() != null && timeslot.getProcedure().getEndTime() != null) {
                                penalty += 1; // 已完成的工序不应再被规划
                            }
                            
                            return penalty;
                        })
                .asConstraint("Fixed start time constraint");
    }
    
    /**
     * 订单开始时间接近度约束（软约束）
     * 确保规划时间尽量接近Order的planStartDate
     * planStartDate默认非空，规划应尽量接近开始时间
     */
    private Constraint orderStartDateProximity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getOrder() != null 
                        && timeslot.getOrder().getPlanStartDate() != null
                        && timeslot.getStartTime() != null)
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
     * 工作中心冲突约束 - 硬约束
     * <p>确保同一工作中心在同一时间不能处理多个任务，防止资源冲突。</p>
     * <p>通过timeslot中的workCenter来分配WorkCenterMaintenance</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配工作中心和开始时间的时间槽</li>
     *   <li>按工作中心分组并检查时间重叠</li>
     *   <li>避免重复计数（通过ID比较确保只检查一次）</li>
     *   <li>对冲突情况应用硬约束惩罚</li>
     * </ol>
     * 
     * @param constraintFactory 约束工厂
     * @return 工作中心冲突约束对象
     */
    private Constraint workCenterConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        Joiners.equal(Timeslot::getWorkCenter),
                        Joiners.overlapping(
                                timeslot -> timeslot.getStartTime() != null ? timeslot.getStartTime() : LocalDateTime.MIN,
                                timeslot -> timeslot.getEndTime() != null ? timeslot.getEndTime() : LocalDateTime.MAX))
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽都有有效的工作中心和开始时间
                    return timeslot1.getWorkCenter() != null && timeslot2.getWorkCenter() != null
                            && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                            && !timeslot1.getId().equals(timeslot2.getId()); // 避免同一个时间槽
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                            // 计算重叠时间（分钟）
                            LocalDateTime start1 = timeslot1.getStartTime();
                            LocalDateTime end1 = timeslot1.getEndTime();
                            LocalDateTime start2 = timeslot2.getStartTime();
                            LocalDateTime end2 = timeslot2.getEndTime();
                            
                            if (end1 == null) {
                                end1 = start1.plusMinutes(timeslot1.getDuration().multiply(BigDecimal.valueOf(60)).longValue());
                            }
                            if (end2 == null) {
                                end2 = start2.plusMinutes(timeslot2.getDuration().multiply(BigDecimal.valueOf(60)).longValue());
                            }
                            
                            LocalDateTime overlapStart = start1.isAfter(start2) ? start1 : start2;
                            LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;
                            
                            if (overlapStart.isBefore(overlapEnd)) {
                                return (int) ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                            }
                            return 0;
                        })
                .asConstraint("Work center conflict");
    }

    /**
     * 工序顺序约束 - 硬约束
     * <p>确保有前后依赖关系的工序按正确顺序执行，防止工序执行顺序错误。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配工序且有后续工序的时间槽（仅最后一个分片）</li>
     *   <li>利用Procedure的nextProcedure链表结构连接前序和后续工序</li>
     *   <li>检查后续工序是否在前置工序完成前开始</li>
     *   <li>对违反顺序的情况应用硬约束惩罚</li>
     * </ol>
     * 
     * @param constraintFactory 约束工厂
     * @return 工序顺序约束对象
     */
    private Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        Joiners.equal(timeslot -> timeslot.getProcedure().getTaskNo()),
                        Joiners.equal(timeslot -> timeslot.getProcedure().getOrderNo()))
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽都有有效的工序和开始时间
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                            // 确保前置工序在后续工序之前完成
                            LocalDateTime endTime1 = timeslot1.getEndTime();
                            if (endTime1 == null) {
                                endTime1 = timeslot1.getStartTime().plusMinutes(
                                        timeslot1.getDuration().multiply(BigDecimal.valueOf(60)).longValue());
                            }
                            
                            LocalDateTime startTime2 = timeslot2.getStartTime();
                            
                            if (endTime1.isAfter(startTime2)) {
                                return (int) ChronoUnit.MINUTES.between(startTime2, endTime1);
                            }
                            return 0;
                        })
                .asConstraint("Procedure sequence constraint");
    }
    
    /**
     * 工作中心维护冲突约束 - 硬约束
     * <p>确保工序不会安排在工作中心维护期间或超出工作中心容量，考虑工作中心状态和使用时间。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>工作中心状态为'n'表示维护中，状态为'y'表示可用</li>
     *   <li>startTime和endTime表示工作中心可以工作的时间段</li>
     *   <li>capacity为通过开始时间和结束时间计算出的最大工作时长（分钟）</li>
     *   <li>usageTime为已使用时间，会根据规划动态变更</li>
     * </ol>
     * 
     * @param constraintFactory 约束工厂
     * @return 工作中心维护冲突约束对象
     */
    private Constraint workCenterMaintenanceConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null 
                        && timeslot.getStartTime() != null
                        && timeslot.getMaintenance() != null)
                .join(WorkCenterMaintenance.class)
                .filter((timeslot, maintenance) -> {
                    // 检查工作中心状态是否为维护中（'n'表示不可用）
                    return maintenance.getWorkCenter().getId().equals(timeslot.getWorkCenter().getId())
                            && maintenance.getStatus() != null && maintenance.getStatus().equals("n");
                })
                .penalize(HardSoftScore.ONE_HARD, (Timeslot timeslot, WorkCenterMaintenance maintenance) -> {
                            // 计算冲突时间
                            LocalDateTime taskStart = timeslot.getStartTime();
                            LocalDateTime taskEnd = timeslot.getEndTime();
                            if (taskEnd == null) {
                                taskEnd = taskStart.plusMinutes(timeslot.getDuration().multiply(BigDecimal.valueOf(60)).longValue());
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
     * 订单优先级约束 - 软约束
     * <p>优先安排高优先级订单的任务，提高客户满意度和业务响应性。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配订单和开始时间的时间槽</li>
     *   <li>根据订单优先级给予奖励（优先级数值越小，奖励越高）</li>
     *   <li>使用较高权重（10）确保优先级得到充分重视</li>
     * </ol>
     * <p><strong>优先级计算：</strong>优先级1（最高）给予100奖励，优先级10给予10奖励，确保高优先级订单得到优先处理。</p>
     * 
     * @param constraintFactory 约束工厂
     * @return 订单优先级最大化约束对象
     */
    private Constraint maximizeOrderPriority(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getOrder() != null 
                        && timeslot.getPriority() != null
                        && timeslot.getStartTime() != null)
                .reward(HardSoftScore.ONE_SOFT.multiply(10), // 使用较高权重确保优先级得到重视
                        timeslot -> {
                            Integer priority = timeslot.getPriority();
                            // 优先级1（最高）给予100奖励，优先级10给予10奖励
                            return Math.max(1, 110 - priority * 10);
                        })
                .asConstraint("Maximize order priority");
    }
    
    /**
     * 机器利用率最大化约束 - 软约束
     * <p>鼓励充分利用设备资源，避免资源浪费，基于工作中心的可用容量计算。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配工作中心和开始时间的时间槽</li>
     *   <li>按工作中心和日期分组，计算每个组的总工作时间（分钟）</li>
     *   <li>利用WorkCenterMaintenance中的capacity作为该工作中心当天的总工作容量</li>
     *   <li>根据实际利用率（已用时间/总容量）给予相应奖励</li>
     * </ol>
     * <p><strong>目标：</strong>最大化机器利用率，使设备使用效率接近其可用容量。</p>
     * 
     * @param constraintFactory 约束工厂
     * @return 机器利用率最大化约束对象
     */
    private Constraint maximizeMachineUtilization(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null 
                        && timeslot.getStartTime() != null)
                .join(WorkCenterMaintenance.class)
                .filter((timeslot, maintenance) -> {
                    // 确保工作中心可用且有容量信息
                    return maintenance.getWorkCenter().getId().equals(timeslot.getWorkCenter().getId())
                            && maintenance.getStatus() != null && maintenance.getStatus().equals("y")
                            && maintenance.getCapacity().compareTo(BigDecimal.ZERO) > 0;
                })
                .groupBy((Timeslot timeslot, WorkCenterMaintenance maintenance) -> new WorkCenterUtilizationKey(
                        timeslot.getWorkCenter().getId(),
                        timeslot.getStartTime().toLocalDate(),
                        maintenance.getCapacity()
                ), ConstraintCollectors.sum((Timeslot timeslot, WorkCenterMaintenance maintenance) -> 
                        timeslot.getDuration().multiply(BigDecimal.valueOf(60)).intValue() // 转换为分钟
                ))
                .reward(HardSoftScore.ONE_SOFT,
                        (WorkCenterUtilizationKey key, Integer totalUsageMinutes) -> {
                            // 计算利用率百分比，最高100%
                            BigDecimal capacityMinutes = key.getCapacity().multiply(BigDecimal.valueOf(60));
                            BigDecimal utilization = BigDecimal.valueOf(totalUsageMinutes)
                                    .divide(capacityMinutes, 2, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .min(BigDecimal.valueOf(100)); // 上限100%
                            
                            return utilization.intValue();
                        })
                .asConstraint("Maximize machine utilization");
    }
    
    // 辅助类，用于分组工作中心利用率数据
    private static class WorkCenterUtilizationKey {
        private final String workCenterId;
        private final LocalDate date;
        private final BigDecimal capacity;
        
        public WorkCenterUtilizationKey(String workCenterId, LocalDate date, BigDecimal capacity) {
            this.workCenterId = workCenterId;
            this.date = date;
            this.capacity = capacity;
        }
        
        public String getWorkCenterId() { return workCenterId; }
        public LocalDate getDate() { return date; }
        public BigDecimal getCapacity() { return capacity; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WorkCenterUtilizationKey that = (WorkCenterUtilizationKey) o;
            return workCenterId.equals(that.workCenterId) && date.equals(that.date);
        }
        
        @Override
        public int hashCode() {
            return workCenterId.hashCode() + date.hashCode();
        }
    }
    
    /**
     * 最小化制造周期约束 - 软约束
     * <p>尽量缩短整体制造周期，使任务尽早开始和完成，提高整体生产效率。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配开始时间的时间槽</li>
     *   <li>计算从当前时间到任务开始时间的分钟数</li>
     *   <li>对开始时间越晚的任务应用越大的惩罚</li>
     *   <li>每延迟30分钟增加1点惩罚</li>
     * </ol>
     * <p><strong>目标：</strong>鼓励尽早安排任务，避免不必要的延迟。</p>
     * 
     * @param constraintFactory 约束工厂
     * @return 制造周期最小化约束对象
     */
    private Constraint minimizeMakespan(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null)
                .penalize(HardSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime taskStartTime = timeslot.getStartTime();
                            
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
     * 工序分片顺序约束
     * 确保工序之间的依赖关系正确，上一道工序的最后一个分片必须在后续工序的第一个分片之前完成
     * 使用Procedure的nextProcedure链表结构替代nextProcedureId字段
     */
    /**
     * 工序分片时间范围约束（硬约束）
     * 确保分片时间在procedure的计划开始时间和结束时间之间
     * 这些计划时间会根据规划实时动态变更
     */
    private Constraint procedureSliceSequence(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        Joiners.equal(timeslot -> timeslot.getProcedure().getId()),
                        Joiners.lessThan(Timeslot::getId)) // 确保处理顺序
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽属于同一工序且有有效的开始时间
                    return timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                            && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                            && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId());
                })
                .penalize(HardSoftScore.ONE_HARD,
                        (timeslot1, timeslot2) -> {
                            // 确保分片按顺序执行：timeslot1在timeslot2之前完成
                            LocalDateTime endTime1 = timeslot1.getEndTime();
                            if (endTime1 == null) {
                                endTime1 = timeslot1.getStartTime().plusMinutes(
                                        timeslot1.getDuration().multiply(BigDecimal.valueOf(60)).longValue());
                            }
                            
                            LocalDateTime startTime2 = timeslot2.getStartTime();
                            
                            if (endTime1.isAfter(startTime2)) {
                                return (int) ChronoUnit.MINUTES.between(startTime2, endTime1);
                            }
                            return 0;
                        })
                .asConstraint("Procedure slice sequence");
    }

    /**
     * 工序分片连续性优化（软约束）
     * 优先让同一工序的分片连续执行，减少时间间隔
     */
    private Constraint procedureSlicePreferContinuous(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        Joiners.equal(timeslot -> timeslot.getProcedure().getId()),
                        Joiners.lessThan(Timeslot::getId)) // 确保处理顺序
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽属于同一工序且有有效的开始时间
                    return timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                            && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                            && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId());
                }).penalize(HardSoftScore.ONE_SOFT,
                        (timeslot1, timeslot2) -> {
                            // 计算两个分片之间的时间间隔
                            LocalDateTime endTime1 = timeslot1.getEndTime();
                            if (endTime1 == null) {
                                endTime1 = timeslot1.getStartTime().plusMinutes(
                                        timeslot1.getDuration().multiply(BigDecimal.valueOf(60)).longValue());
                            }
                            
                            LocalDateTime startTime2 = timeslot2.getStartTime();
                            
                            if (endTime1.isBefore(startTime2)) {
                                // 计算时间间隔（分钟），间隔越大惩罚越大
                                return (int) ChronoUnit.MINUTES.between(endTime1, startTime2);
                            }
                            return 0;
                        })
                .asConstraint("Procedure slice prefer continuous");
    }
}
