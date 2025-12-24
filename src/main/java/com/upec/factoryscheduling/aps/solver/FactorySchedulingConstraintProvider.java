package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.*;

@Slf4j
@Component
public class FactorySchedulingConstraintProvider implements ConstraintProvider, Serializable {
    private static final long serialVersionUID = 1L;

    // 性能优化：缓存常量值
    private static final String STATUS_UNAVAILABLE = "N";
    private static final String STATUS_AVAILABLE = "Active";
    private static final int MINUTES_PER_DAY = 480;
    private static final int PLANNING_HORIZON_DAYS = 30;
    private static final int AVERAGE_DAILY_LOAD = MINUTES_PER_DAY * PLANNING_HORIZON_DAYS;
    private static final int CAPACITY_BUFFER = 60; // 每天预留60分钟缓冲

    // 权重常数
    private static final int HARD_PENALTY_WEIGHT = 1000;
    private static final int MEDIUM_PENALTY_WEIGHT = 100;
    private static final int SOFT_REWARD_WEIGHT = 10;

    private static final String WORK_CENTER_CODE = "PM10W200";

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // ============ 硬约束 (必须满足) ============
                // 基本业务规则违反 - 最高优先级
                hardWorkCenterMatch(constraintFactory),
                hardCapacityExceeded(constraintFactory),
                hardOutsourcingProcedurePreviousTimeConstraint(constraintFactory),
                hardOutsourcingProcedureNextTimeConstraint(constraintFactory),

                // ============ 中等约束 (尽量满足) ============
                // 重要业务规则 - 中等优先级
                mediumProcedureSequence(constraintFactory),
                mediumProcedureSliceSequence(constraintFactory),
                mediumOrderDateConstraint(constraintFactory),

                // ============ 软约束 (优化目标) ============
                // 业务优化目标 - 低优先级
                softEarlyCompletion(constraintFactory),
                softOnTimeStart(constraintFactory),
                softHighPriorityFirst(constraintFactory),
                softBalancedLoad(constraintFactory),
                softContinuousSlices(constraintFactory),
                softCapacityUtilization(constraintFactory)
        };
    }

    /**
     * 硬约束1: 工作中心必须匹配
     * 违反条件：为维护任务分配了错误的工作中心
     */
    protected Constraint hardWorkCenterMatch(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot ->
                        timeslot.getMaintenance() != null
                                && timeslot.getWorkCenter() != null
                                && !timeslot.getMaintenance().getWorkCenter().getId().equals(timeslot.getWorkCenter().getId()))
                .penalize(HardMediumSoftScore.ONE_HARD, timeslot -> HARD_PENALTY_WEIGHT * 10) // 严重违反
                .asConstraint("硬约束：工作中心必须匹配");
    }

    /**
     * 硬约束2: 不能超过维护容量
     * 违反条件：分配给某天维护的任务总时长超过维护容量
     * 注意：工作中心为PM10W200的外协工序不考虑容量约束
     */
    protected Constraint hardCapacityExceeded(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null && timeslot.getDuration() > 0 &&
                        (timeslot.getWorkCenter() == null || !WORK_CENTER_CODE.equals(timeslot.getWorkCenter().getWorkCenterCode())))
                .groupBy(Timeslot::getMaintenance, sum(Timeslot::getDuration))
                .filter((maintenance, totalDuration) ->
                        totalDuration + maintenance.getUsageTime() > maintenance.getCapacity())
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (maintenance, totalDuration) -> {
                            int exceeded = totalDuration + maintenance.getUsageTime() - maintenance.getCapacity();
                            return exceeded * HARD_PENALTY_WEIGHT;
                        })
                .asConstraint("硬约束：不能超过维护容量");
    }

    /**
     * 硬约束5: 外协工序时间约束 - 上一道工序结束时间必须等于该工序开始时间
     * 仅适用于工作中心为PM10W200的外协工序
     */
    protected Constraint hardOutsourcingProcedurePreviousTimeConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null &&
                        WORK_CENTER_CODE.equals(timeslot.getWorkCenter().getWorkCenterCode()) &&
                        timeslot.getProcedure() != null &&
                        timeslot.getProcedure().getProcedureNo() > 1 &&
                        timeslot.getStartTime() != null)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getTask().getTaskNo(), t -> t.getTask().getTaskNo()),
                        Joiners.equal(t -> t.getProcedure().getIndex() - 1, t -> t.getProcedure().getIndex()))
                .filter((current, previous) -> previous.getEndTime() != null && !previous.getEndTime().equals(current.getStartTime()))
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (current, previous) -> HARD_PENALTY_WEIGHT * 10)
                .asConstraint("硬约束：外协工序-上一道工序结束时间必须等于该工序开始时间");
    }

    /**
     * 硬约束6: 外协工序时间约束 - 该工序结束时间必须等于下一道工序开始时间
     * 仅适用于工作中心为PM10W200的外协工序
     */
    protected Constraint hardOutsourcingProcedureNextTimeConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null &&
                        WORK_CENTER_CODE.equals(timeslot.getWorkCenter().getWorkCenterCode()) &&
                        timeslot.getProcedure() != null &&
                        timeslot.getProcedure().getNextProcedureNo() != null && !
                        timeslot.getProcedure().getNextProcedureNo().isEmpty() &&
                        timeslot.getEndTime() != null)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getTask().getTaskNo(), t -> t.getTask().getTaskNo()),
                        Joiners.filtering((current, next) -> next.getProcedure() != null &&
                                current.getProcedure().getNextProcedureNo().contains(next.getProcedure().getProcedureNo())))
                .filter((current, next) -> next.getStartTime() != null && !current.getEndTime().equals(next.getStartTime()))
                .penalize(HardMediumSoftScore.ONE_HARD, (current, next) -> HARD_PENALTY_WEIGHT * 10)
                .asConstraint("硬约束：外协工序-该工序结束时间必须等于下一道工序开始时间");
    }

    /**
     * 中等约束1: 工序顺序约束
     * 违反条件：后序工序在前序工序完成前开始
     */
    protected Constraint mediumProcedureSequence(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getProcedure() != null &&
                        timeslot.getEndTime() != null && !
                        timeslot.getProcedure().getNextProcedure().isEmpty())
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getTask().getTaskNo(), t -> t.getTask().getTaskNo()),
                        Joiners.filtering((current, next) -> current.getProcedure().getNextProcedure().stream().anyMatch(p -> p.getId().equals(next.getProcedure().getId()))))
                .filter((current, next) ->
                        next.getStartTime() != null && !current.getEndTime().isBefore(next.getStartTime()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (current, next) -> {
                            // 如果后序在前序完成前开始，计算提前的时间
                            long minutesEarly = Duration.between(next.getStartTime(), current.getEndTime()).toMinutes();
                            return (int) Math.max(0, minutesEarly) * MEDIUM_PENALTY_WEIGHT;
                        })
                .asConstraint("中约束：工序必须按顺序执行");
    }

    /**
     * 中等约束2: 工序分片顺序约束
     * 违反条件：同一工序的分片顺序错误
     */
    protected Constraint mediumProcedureSliceSequence(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getTotal() > 1 && timeslot.getIndex() < timeslot.getTotal() - 1)
                .join(Timeslot.class,
                        Joiners.equal(Timeslot::getProcedure),
                        Joiners.equal(t -> t.getIndex() + 1, Timeslot::getIndex))
                .filter((slice1, slice2) ->
                        slice2.getStartTime() != null &&
                                slice1.getEndTime() != null && !
                                slice1.getEndTime().isBefore(slice2.getStartTime()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (slice1, slice2) -> MEDIUM_PENALTY_WEIGHT * 5)
                .asConstraint("中约束：同一工序分片必须按顺序执行");
    }

    /**
     * 中等约束3: 订单日期约束
     * 违反条件：任务开始时间早于实际开始时间
     */
    protected Constraint mediumOrderDateConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot ->
                        timeslot.getTask() != null
                                && timeslot.getTask().getFactStartDate() != null
                                && timeslot.getStartTime() != null
                                && timeslot.getStartTime().isBefore(timeslot.getTask().getFactStartDate()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        timeslot -> {
                            long daysEarly = Duration.between(timeslot.getStartTime(), timeslot.getTask().getFactStartDate()).toDays();
                            return (int) daysEarly * MEDIUM_PENALTY_WEIGHT;
                        })
                .asConstraint("中约束：不能早于实际开始时间");
    }

    /**
     * 软约束1: 奖励提前完成
     * 优化目标：越早完成越好
     */
    protected Constraint softEarlyCompletion(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getEndTime() != null &&
                        timeslot.getProcedure() != null &&
                        timeslot.getProcedure().getPlanEndDate() != null)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDateTime planEnd = timeslot.getProcedure().getPlanEndDate().atTime(23, 59);
                            LocalDateTime actualEnd = timeslot.getEndTime();
                            if (actualEnd.isBefore(planEnd)) {
                                long daysEarly = Duration.between(actualEnd, planEnd).toDays();
                                return (int) daysEarly * SOFT_REWARD_WEIGHT;
                            }
                            return 0;
                        })
                .asConstraint("软约束：奖励提前完成");
    }

    /**
     * 软约束2: 奖励准时开始
     * 优化目标：按计划时间开始
     */
    protected Constraint softOnTimeStart(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot ->
                        timeslot.getStartTime() != null
                                && timeslot.getTask() != null
                                && timeslot.getTask().getPlanStartDate() != null
                                && timeslot.getProcedureIndex() == 1)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDateTime planStart = timeslot.getTask().getPlanStartDate().atStartOfDay();
                            LocalDateTime actualStart = timeslot.getStartTime();
                            long hoursDiff = Math.abs(Duration.between(planStart, actualStart).toHours());
                            if (hoursDiff <= 4) {
                                return (int) (SOFT_REWARD_WEIGHT * (5 - hoursDiff));
                            }
                            return 0;
                        })
                .asConstraint("软约束：奖励准时开始");
    }

    /**
     * 软约束3: 奖励高优先级任务
     * 优化目标：优先安排高优先级任务
     */
    protected Constraint softHighPriorityFirst(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getPriority() != null && timeslot.getEndTime() != null)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            int priority = timeslot.getPriority();
                            LocalDateTime now = LocalDateTime.now();
                            if (priority <= 3) {
                                long daysFromNow = Duration.between(now, timeslot.getEndTime()).toDays();
                                if (daysFromNow >= 0 && daysFromNow <= 7) {
                                    return (4 - priority) * SOFT_REWARD_WEIGHT * 2;
                                }
                            }
                            return 0;
                        })
                .asConstraint("软约束：奖励高优先级任务先完成");
    }

    /**
     * 软约束4: 奖励均衡负载
     * 优化目标：工作中心负载均衡
     */
    protected Constraint softBalancedLoad(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot ->
                        timeslot.getWorkCenter() != null && timeslot.getDuration() > 0)
                .groupBy(Timeslot::getWorkCenter, sum(Timeslot::getDuration))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (workCenter, totalDuration) -> {
                            int deviation = Math.abs(totalDuration - AVERAGE_DAILY_LOAD);
                            int maxDeviation = AVERAGE_DAILY_LOAD / 4; // 允许25%偏差
                            if (deviation < maxDeviation) {
                                return (maxDeviation - deviation) / 100;
                            }
                            return 0;
                        })
                .asConstraint("软约束：奖励均衡负载");
    }

    /**
     * 软约束5: 奖励连续分片
     * 优化目标：同一工序的分片连续执行
     */
    protected Constraint softContinuousSlices(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot ->
                        timeslot.getTotal() > 1 && timeslot.getIndex() < timeslot.getTotal() - 1)
                .join(Timeslot.class,
                        Joiners.equal(Timeslot::getProcedure),
                        Joiners.equal(t -> t.getIndex() + 1, Timeslot::getIndex))
                .filter((slice1, slice2) -> slice2.getStartTime() != null && slice1.getEndTime() != null)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (slice1, slice2) -> {
                            long gapMinutes = Duration.between(slice1.getEndTime(), slice2.getStartTime()).toMinutes();
                            if (gapMinutes <= 30) {
                                return SOFT_REWARD_WEIGHT * 3;
                            } else if (gapMinutes <= 60) {
                                return SOFT_REWARD_WEIGHT;
                            }
                            return 0;
                        })
                .asConstraint("软约束：奖励连续分片");
    }

    /**
     * 软约束6: 奖励合理容量利用
     * 优化目标：合理利用维护容量，不过度也不浪费
     */
    protected Constraint softCapacityUtilization(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null && timeslot.getDuration() > 0)
                .groupBy(Timeslot::getMaintenance, sum(Timeslot::getDuration))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (maintenance, totalDuration) -> {
                            int used = totalDuration + maintenance.getUsageTime();
                            int capacity = maintenance.getCapacity();
                            int optimalMin = (int) (capacity * 0.8);
                            int optimalMax = (int) (capacity * 0.9);
                            if (used >= optimalMin && used <= optimalMax) {
                                return SOFT_REWARD_WEIGHT * 5;
                            } else if (used >= optimalMin * 0.8 && used <= optimalMax * 1.2) {
                                return SOFT_REWARD_WEIGHT * 2;
                            }
                            return 0;
                        })
                .asConstraint("软约束：奖励合理容量利用");
    }
}
