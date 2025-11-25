package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
                workCenterMaintenanceAllocationConstraint(constraintFactory),
                procedureSequenceConstraint(constraintFactory),
                timeslotIndexOrderConstraint(constraintFactory)
        };
    }



    /**
     * 工作中心维护分配约束 - 硬约束
     * <p>确保时间槽分配的工作中心维护计划与工作中心匹配，防止错误分配维护计划。</p>
     * <p><strong>约束条件：</strong></p>
     * <ul>
     *   <li>timeslot.getWorkCenter().getId().equals(maintenance.getWorkCenter().getId())</li>
     * </ul>
     * <p>当时间槽被分配了维护计划时，必须确保维护计划对应的工作中心与时间槽的工作中心一致。</p>
     *
     * @param constraintFactory 约束工厂
     * @return 工作中心维护分配约束对象
     **/
    /**
     * 工作中心维护计划分配约束
     * 确保Timeslot只能分配给具有相同workCenter Id的WorkCenterMaintenance，并且当workCenter为空时不能分配maintenance
     */
    private Constraint workCenterMaintenanceAllocationConstraint(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null)
                .filter(timeslot -> {
                    // 当workCenter为空时违反约束
                    if (timeslot.getWorkCenter() == null) {
                        return true;
                    }
                    // 当工作中心ID不匹配时违反约束
                    return timeslot.getMaintenance().getWorkCenter() != null &&
                            !timeslot.getWorkCenter().getId().equals(timeslot.getMaintenance().getWorkCenter().getId());
                })
                // 违反约束时施加硬约束惩罚
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Work Center Maintenance Allocation Constraint");
    }

    /**
     * 工序分片索引顺序约束（硬约束）
     * 确保隶属同一个工序的时间槽时间顺序必须按照时间槽索引确定开始时间顺序
     * 索引较小的时间槽的开始时间必须早于索引较大的时间槽
     */
    private Constraint timeslotIndexOrderConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        // 确保两个时间槽属于同一工序
                        Joiners.equal(timeslot -> timeslot.getProcedure() != null ? timeslot.getProcedure().getId() : null),
                        // 只比较不同的时间槽
                        Joiners.filtering((timeslot1, timeslot2) -> {
                            // 确保两个时间槽都有关联的工序和有效的索引
                            return timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                                    && timeslot1.getIndex() != null && timeslot2.getIndex() != null
                                    // 确保索引不同，避免比较同一个时间槽或索引相同的时间槽
                                    && !timeslot1.getIndex().equals(timeslot2.getIndex())
                                    // 确保两个时间槽都有有效的开始时间
                                    && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null;
                        }))
                .filter((timeslot1, timeslot2) -> {
                    // 索引较小的时间槽的开始时间不应晚于索引较大的时间槽的开始时间
                    Integer index1 = timeslot1.getIndex();
                    Integer index2 = timeslot2.getIndex();
                    LocalDateTime startTime1 = timeslot1.getStartTime();
                    LocalDateTime startTime2 = timeslot2.getStartTime();
                    
                    // 如果索引较小的时间槽开始时间反而晚，违反约束
                    return (index1 < index2 && startTime1.isAfter(startTime2)) ||
                           (index1 > index2 && startTime1.isBefore(startTime2));
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                    // 计算违反约束的时间偏差（分钟）
                    LocalDateTime startTime1 = timeslot1.getStartTime();
                    LocalDateTime startTime2 = timeslot2.getStartTime();
                    LocalDateTime earlierTime = startTime1.isBefore(startTime2) ? startTime1 : startTime2;
                    LocalDateTime laterTime = startTime1.isBefore(startTime2) ? startTime2 : startTime1;
                    
                    return (int) ChronoUnit.MINUTES.between(earlierTime, laterTime);
                })
                .asConstraint("Timeslot index order constraint");
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
    /**
     * 工序顺序约束（硬约束）
     * <p>确保：</p>
     * <ul>
     *   <li>同一个订单的不同工序必须按照顺序执行</li>
     *   <li>前置工序必须在后续工序之前完成</li>
     *   <li>支持并行工序：当nextProcedure的size大于1时，这些工序可以并行执行</li>
     * </ul>
     */
    private Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                Joiners.equal(timeslot -> timeslot.getProcedure() != null ? timeslot.getProcedure().getOrderNo() : null)
        ).filter((timeslot1, timeslot2) -> {
            // 确保两个时间槽都有有效的工序和开始时间
            if (timeslot1.getProcedure() == null || timeslot2.getProcedure() == null) {
                return false;
            }
            if (timeslot1.getStartTime() == null || timeslot2.getStartTime() == null) {
                return false;
            }
            // 检查t2是否是t1的后续工序
            return isNextProcedure(timeslot1.getProcedure(), timeslot2.getProcedure());
        }).penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
            // 确保前置工序在后续工序之前完成
            // 这里不使用plusDays(1)，因为工序可能在同一天完成和开始
            LocalDateTime endTime1 = timeslot1.getStartTime(); // 简化处理，实际可能需要根据工序时长计算结束时间
            LocalDateTime startTime2 = timeslot2.getStartTime();
            if (endTime1.isAfter(startTime2)) {
                // 计算违反约束的时间量（分钟）作为惩罚值
                return (int) ChronoUnit.MINUTES.between(startTime2, endTime1);
            }
            return 0;
        }).asConstraint("Procedure sequence constraint");
    }

    /**
     * 检查procedure2是否是procedure1的后续工序
     */
    private boolean isNextProcedure(Procedure procedure1, Procedure procedure2) {
        // 获取procedure1的所有后续工序
        List<Procedure> nextProcedures = procedure1.getNextProcedure();
        if (nextProcedures == null || nextProcedures.isEmpty()) {
            return false;
        }
        
        // 检查procedure2是否在procedure1的后续工序列表中
        for (Procedure nextProcedure : nextProcedures) {
            if (procedure2.getId().equals(nextProcedure.getId())) {
                return true;
            }
        }
        return false;
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
                .filter(this::orderPlanDateNotNull)
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

    private boolean orderPlanDateNotNull(Timeslot timeslot) {
        return timeslot.getOrder().getPlanStartDate() != null && timeslot.getOrder().getPlanEndDate() != null & timeslot.getStartTime() != null;
    }

    /**
     * 工作中心冲突约束 - 硬约束
     * <p>确保同一工作中心在同一天的容量固定，防止资源冲突。</p>
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
                        Joiners.equal(Timeslot::getMaintenance),
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
                        end1 = start1.plusMinutes((long) (timeslot1.getDuration() * 60));
                    }
                    if (end2 == null) {
                        end2 = start2.plusMinutes((long) (timeslot2.getDuration() * 60));
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
                }).penalize(HardSoftScore.ONE_HARD, (Timeslot timeslot, WorkCenterMaintenance maintenance) -> {
                    // 计算冲突时间
                    LocalDateTime taskStart = timeslot.getStartTime();
                    LocalDateTime taskEnd = timeslot.getEndTime();
                    if (taskEnd == null) {
                        taskEnd = taskStart.plusMinutes((long) (timeslot.getDuration() * 60));
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
                            && maintenance.getCapacity().doubleValue() > 0.0;
                })
                .groupBy((Timeslot timeslot, WorkCenterMaintenance maintenance) -> new WorkCenterUtilizationKey(
                        timeslot.getWorkCenter().getId(),
                        timeslot.getStartTime().toLocalDate(),
                        maintenance.getCapacity().doubleValue()
                ), ConstraintCollectors.sum((Timeslot timeslot, WorkCenterMaintenance maintenance) ->
                        (int) (timeslot.getDuration() * 60) // 转换为分钟
                ))
                .reward(HardSoftScore.ONE_SOFT,
                        (WorkCenterUtilizationKey key, Integer totalUsageMinutes) -> {
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
                }).penalize(HardSoftScore.ONE_HARD,
                        (timeslot1, timeslot2) -> {
                            // 确保分片按顺序执行：timeslot1在timeslot2之前完成
                            LocalDateTime endTime1 = timeslot1.getEndTime();
                            if (endTime1 == null) {
                                endTime1 = timeslot1.getStartTime().plusMinutes(
                                        (long) (timeslot1.getDuration() * 60));
                            }
                            LocalDateTime startTime2 = timeslot2.getStartTime();
                            if (endTime1.isAfter(startTime2)) {
                                return (int) ChronoUnit.MINUTES.between(startTime2, endTime1);
                            }
                            return 0;
                        }).asConstraint("Procedure slice sequence");
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
                                        (long) (timeslot1.getDuration() * 60));
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
