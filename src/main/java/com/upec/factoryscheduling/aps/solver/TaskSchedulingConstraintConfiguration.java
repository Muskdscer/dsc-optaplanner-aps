package com.upec.factoryscheduling.aps.solver;

import org.optaplanner.core.api.domain.constraintweight.ConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.springframework.stereotype.Component;

@Component
@ConstraintConfiguration(constraintPackage = "com.upec.factoryscheduling.aps.solver")
public class TaskSchedulingConstraintConfiguration {

    // ============ 硬约束权重 ============
    @ConstraintWeight("硬约束：工作中心必须匹配")
    private HardMediumSoftScore workCenterMatch = HardMediumSoftScore.ofHard(-10000);

    @ConstraintWeight("硬约束：工作中心必须可用")
    private HardMediumSoftScore workCenterAvailability = HardMediumSoftScore.ofHard(-8000);

    @ConstraintWeight("硬约束：同一工作中心时间不能重叠")
    private HardMediumSoftScore noOverlap = HardMediumSoftScore.ofHard(-5000);

    @ConstraintWeight("硬约束：不能超过维护容量")
    private HardMediumSoftScore capacityExceeded = HardMediumSoftScore.ofHard(-6000);

    // ============ 中等约束权重 ============
    @ConstraintWeight("中约束：工序必须按顺序执行")
    private HardMediumSoftScore procedureSequence = HardMediumSoftScore.ofMedium(-200);

    @ConstraintWeight("中约束：同一工序分片必须按顺序执行")
    private HardMediumSoftScore sliceSequence = HardMediumSoftScore.ofMedium(-150);

    @ConstraintWeight("中约束：不能早于实际开始时间")
    private HardMediumSoftScore orderDate = HardMediumSoftScore.ofMedium(-100);

    // ============ 软约束权重 ============
    @ConstraintWeight("软约束：奖励提前完成")
    private HardMediumSoftScore earlyCompletion = HardMediumSoftScore.ofSoft(50);

    @ConstraintWeight("软约束：奖励准时开始")
    private HardMediumSoftScore onTimeStart = HardMediumSoftScore.ofSoft(40);

    @ConstraintWeight("软约束：奖励高优先级任务先完成")
    private HardMediumSoftScore highPriorityFirst = HardMediumSoftScore.ofSoft(60);

    @ConstraintWeight("软约束：奖励均衡负载")
    private HardMediumSoftScore balancedLoad = HardMediumSoftScore.ofSoft(30);

    @ConstraintWeight("软约束：奖励连续分片")
    private HardMediumSoftScore continuousSlices = HardMediumSoftScore.ofSoft(25);

    @ConstraintWeight("软约束：奖励合理容量利用")
    private HardMediumSoftScore capacityUtilization = HardMediumSoftScore.ofSoft(20);

    // ============ Getter/Setter ============
    // 省略...

    /**
     * 根据业务场景调整权重
     */
    public void adjustForScenario(SchedulingScenario scenario) {
        switch (scenario) {
            case EMERGENCY:
                // 紧急情况：强调时效性
                earlyCompletion = HardMediumSoftScore.ofSoft(100);
                highPriorityFirst = HardMediumSoftScore.ofSoft(80);
                capacityExceeded = HardMediumSoftScore.ofHard(-8000);
                break;

            case COST_OPTIMIZATION:
                // 成本优化：强调资源利用率
                capacityUtilization = HardMediumSoftScore.ofSoft(60);
                balancedLoad = HardMediumSoftScore.ofSoft(50);
                break;

            case QUALITY:
                // 质量优先：强调顺序和可用性
                procedureSequence = HardMediumSoftScore.ofMedium(-300);
                workCenterAvailability = HardMediumSoftScore.ofHard(-10000);
                break;

            default:
                // 保持默认
                break;
        }
    }

    public enum SchedulingScenario {
        EMERGENCY,
        COST_OPTIMIZATION,
        QUALITY,
        BALANCED
    }
}
