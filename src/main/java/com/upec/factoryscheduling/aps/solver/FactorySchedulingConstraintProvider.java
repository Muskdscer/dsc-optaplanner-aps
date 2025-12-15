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
import java.time.LocalDateTime;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;


@Slf4j
@Component
public class FactorySchedulingConstraintProvider implements ConstraintProvider, Serializable {
    private static final long serialVersionUID = 1L;

    // 性能优化：缓存常量值
    private static final String STATUS_UNAVAILABLE = "N";
    private static final String STATUS_AVAILABLE = "Y";
    private static final int MINUTES_PER_DAY = 480;
    private static final int PLANNING_HORIZON_DAYS = 30;
    private static final int AVERAGE_DAILY_LOAD = MINUTES_PER_DAY * PLANNING_HORIZON_DAYS;
    private static final int CAPACITY_BUFFER = 60; // 每天预留60分钟缓冲

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // 硬约束 - 按重要性和计算复杂度排序
                workCenterMatch(constraintFactory),
                orderDateConstraint(constraintFactory),
                machineCapacityConstraint(constraintFactory),
                noOverlappingTimeslots(constraintFactory),
                procedureSequenceConstraint(constraintFactory),
                procedureSliceSequenceConstraint(constraintFactory),

                // 软约束 - 使用reward优化
                rewardEarlyCompletion(constraintFactory),
                rewardOnTimeStart(constraintFactory),
                rewardHighPriorityEarly(constraintFactory),
                rewardBalancedLoad(constraintFactory),
                rewardContinuousSlices(constraintFactory),
                rewardCapacityBuffer(constraintFactory)
        };
    }

    /**
     * 硬约束1: 工作中心约束 - 优化版
     * 使用reward奖励正确匹配，而不是惩罚错误
     */
    protected Constraint workCenterMatch(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getWorkCenter() != null)
                .reward(HardMediumSoftScore.ONE_HARD,
                        timeslot -> timeslot.getMaintenance().getWorkCenter().getId()
                                .equals(timeslot.getWorkCenter().getId()) ? 1 : 0)
                .asConstraint("工作中心匹配奖励");
    }

    /**
     * 硬约束2: 工作中心可用性
     * 使用reward奖励使用可用的工作中心
     */
    protected Constraint workCenterStatusAvailable(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null)
                .reward(HardMediumSoftScore.ONE_HARD,
                        timeslot -> STATUS_AVAILABLE.equals(timeslot.getWorkCenter().getStatus()) ? 1 : 0)
                .asConstraint("使用可用工作中心奖励");
    }

    /**
     * 硬约束3: 机器容量约束 - reward版本
     * 奖励剩余容量，而不是惩罚超出容量
     */
    protected Constraint machineCapacityConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getDuration() > 0)
                .groupBy(
                        Timeslot::getMaintenance,
                        sum(Timeslot::getDuration)
                )
                .reward(HardMediumSoftScore.ONE_HARD,
                        (maintenance, totalDuration) -> {
                            int remaining = maintenance.getCapacity() - totalDuration - maintenance.getUsageTime();
                            // 有剩余容量就奖励，超出就惩罚
                            return remaining >= 0 ? remaining / 10 : 0;
                        })
                .asConstraint("容量内使用奖励");
    }

    /**
     * 硬约束4: 工序顺序约束 - reward版本
     * 奖励正确的工序顺序
     */
    protected Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getProcedure() != null
                        && timeslot.getEndTime() != null
                        && timeslot.getProcedure().getNextProcedure() != null
                        && !timeslot.getProcedure().getNextProcedure().isEmpty())
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getTask().getTaskNo(), t -> t.getTask().getTaskNo()),
                        Joiners.lessThan(t -> t.getProcedure().getProcedureNo(),
                                t -> t.getProcedure().getProcedureNo()),
                        Joiners.filtering((current, next) -> {
                            if (next.getProcedure() == null || next.getStartTime() == null) {
                                return false;
                            }
                            String nextProcId = next.getProcedure().getId();
                            return current.getProcedure().getNextProcedure().stream()
                                    .anyMatch(p -> p.getId().equals(nextProcId));
                        }))
                .reward(HardMediumSoftScore.ONE_HARD,
                        (current, next) -> {
                            // 奖励正确的顺序（前序结束早于后序开始）
                            return current.getEndTime().isBefore(next.getStartTime()) ||
                                    current.getEndTime().equals(next.getStartTime()) ? 1 : 0;
                        })
                .asConstraint("正确工序顺序奖励");
    }

    /**
     * 硬约束5: 同一工序的时间片顺序 - reward版本
     */
    protected Constraint procedureSliceSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getTotal() > 1
                        && timeslot.getIndex() < timeslot.getTotal()
                        && timeslot.getEndTime() != null
                        && timeslot.getProcedure() != null)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getProcedure().getId(), t -> t.getProcedure().getId()),
                        Joiners.equal(t -> t.getIndex() + 1, Timeslot::getIndex))
                .reward(HardMediumSoftScore.ONE_HARD,
                        (slice1, slice2) -> {
                            if (slice2.getStartTime() == null) return 0;
                            // 奖励正确的片段顺序
                            return slice1.getEndTime().isBefore(slice2.getStartTime()) ||
                                    slice1.getEndTime().equals(slice2.getStartTime()) ? 1 : 0;
                        })
                .asConstraint("正确分片顺序奖励");
    }

    /**
     * 硬约束6: 订单日期约束 - reward版本
     */
    protected Constraint orderDateConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getTask() != null
                        && timeslot.getTask().getFactStartDate() != null
                        && timeslot.getProcedureIndex() == 1
                        && timeslot.getStartTime() != null)
                .reward(HardMediumSoftScore.ONE_HARD,
                        timeslot -> {
                            // 奖励符合实际开始时间的安排
                            return !timeslot.getStartTime().isBefore(timeslot.getTask().getFactStartDate()) ? 1 : 0;
                        })
                .asConstraint("符合订单开始日期奖励");
    }

    /**
     * 硬约束7: 时间重叠约束 - reward版本
     * 奖励不重叠的时间槽配置
     */
    protected Constraint noOverlappingTimeslots(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null
                        && timeslot.getEndTime() != null
                        && timeslot.getWorkCenter() != null)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getWorkCenter().getId(), t -> t.getWorkCenter().getId()),
                        Joiners.lessThan(Timeslot::getId),
                        Joiners.overlapping(
                                Timeslot::getStartTime,
                                Timeslot::getEndTime,
                                Timeslot::getStartTime,
                                Timeslot::getEndTime
                        ))
                // 找到重叠就不奖励（等同于惩罚）
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("避免时间重叠");
    }

    // ==================== 软约束 - 全部使用reward ====================

    /**
     * 软约束1: 奖励提前完成
     * 越早完成奖励越多
     */
    protected Constraint rewardEarlyCompletion(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getEndTime() != null
                        && timeslot.getProcedure() != null
                        && timeslot.getProcedure().getPlanEndDate() != null)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDateTime planEnd = timeslot.getProcedure().getPlanEndDate()
                                    .atTime(23, 59);
                            LocalDateTime actualEnd = timeslot.getEndTime();
                            if (actualEnd.isBefore(planEnd)) {
                                // 提前完成，奖励天数差
                                long daysEarly = java.time.Duration.between(actualEnd, planEnd).toDays();
                                return (int) Math.min(daysEarly * 2, 100);
                            }
                            return 0;
                        })
                .asConstraint("提前完成奖励");
    }

    /**
     * 软约束2: 奖励准时开始
     * 按计划开始时间开始的任务获得奖励
     */
    protected Constraint rewardOnTimeStart(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null
                        && timeslot.getTask() != null
                        && timeslot.getTask().getPlanStartDate() != null
                        && timeslot.getProcedureIndex() == 1)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDateTime planStart = timeslot.getTask().getPlanStartDate().atStartOfDay();
                            LocalDateTime actualStart = timeslot.getStartTime();
                            long hoursDiff = Math.abs(java.time.Duration.between(planStart, actualStart).toHours());
                            // 在计划时间±24小时内开始，给予奖励
                            if (hoursDiff <= 24) {
                                return 50 - (int) (hoursDiff / 2);
                            }
                            return 0;
                        })
                .asConstraint("准时开始奖励");
    }

    /**
     * 软约束3: 奖励高优先级订单早完成
     * 优先级越高，越早完成，奖励越多
     */
    protected Constraint rewardHighPriorityEarly(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getPriority() != null
                        && timeslot.getEndTime() != null
                        && timeslot.getProcedureIndex() == timeslot.getProcedure().getLevel())
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            // 高优先级（数字小）越早完成奖励越多
                            int priority = Math.min(timeslot.getPriority(), 10);
                            LocalDateTime now = LocalDateTime.now();
                            long daysFromNow = java.time.Duration.between(now, timeslot.getEndTime()).toDays();

                            // 30天内完成才奖励，优先级1最高奖励
                            if (daysFromNow >= 0 && daysFromNow <= 30) {
                                int reward = (11 - priority) * (31 - (int) daysFromNow);
                                return Math.max(0, reward / 5);
                            }
                            return 0;
                        })
                .asConstraint("高优先级早完成奖励");
    }

    /**
     * 软约束4: 奖励均衡的机器负载
     * 负载接近平均值的机器获得奖励
     */
    protected Constraint rewardBalancedLoad(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null
                        && timeslot.getDuration() > 0)
                .groupBy(Timeslot::getWorkCenter,
                        sum(Timeslot::getDuration))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (workCenter, totalDuration) -> {
                            // 奖励接近平均负载的机器
                            int deviation = Math.abs(totalDuration - AVERAGE_DAILY_LOAD);
                            // 偏差越小奖励越多
                            int maxDeviation = AVERAGE_DAILY_LOAD / 2;
                            if (deviation < maxDeviation) {
                                return (maxDeviation - deviation) / 500;
                            }
                            return 0;
                        })
                .asConstraint("均衡负载奖励");
    }

    /**
     * 软约束5: 奖励连续的时间片
     * 同一工序的时间片越紧密，奖励越多
     */
    protected Constraint rewardContinuousSlices(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getTotal() > 1
                        && timeslot.getIndex() < timeslot.getTotal()
                        && timeslot.getEndTime() != null
                        && timeslot.getProcedure() != null)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getProcedure().getId(), t -> t.getProcedure().getId()),
                        Joiners.equal(t -> t.getIndex() + 1, Timeslot::getIndex))
                .filter((slice1, slice2) -> slice2.getStartTime() != null
                        && !slice2.getStartTime().isBefore(slice1.getEndTime()))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (slice1, slice2) -> {
                            long intervalHours = java.time.Duration.between(
                                    slice1.getEndTime(),
                                    slice2.getStartTime()).toHours();
                            // 间隔越短奖励越多
                            if (intervalHours == 0) {
                                return 100; // 连续执行最高奖励
                            } else if (intervalHours <= 24) {
                                return 50 - (int) (intervalHours * 2);
                            }
                            return 0;
                        })
                .asConstraint("连续分片奖励");
    }

    /**
     * 软约束6: 奖励预留容量缓冲
     * 为每天预留适当的缓冲时间
     */
    protected Constraint rewardCapacityBuffer(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getDuration() > 0)
                .groupBy(
                        Timeslot::getMaintenance,
                        sum(Timeslot::getDuration)
                )
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (maintenance, totalDuration) -> {
                            int remaining = maintenance.getCapacity() - totalDuration;
                            // 奖励保留60-120分钟缓冲
                            if (remaining >= CAPACITY_BUFFER && remaining <= CAPACITY_BUFFER * 2) {
                                return remaining / 10;
                            } else if (remaining > CAPACITY_BUFFER * 2) {
                                // 预留太多也不好，轻微奖励
                                return 5;
                            }
                            return 0;
                        })
                .asConstraint("合理容量缓冲奖励");
    }
}
