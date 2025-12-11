package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;


@Slf4j
@Component
public class FactorySchedulingConstraintProvider implements ConstraintProvider, Serializable {
    private static final long serialVersionUID = 1L;


    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{

        };
    }

    /**
     * 硬约束1: 工作中心约束
     * 工作中心和工作中心日历必须匹配
     * 工作中心状态为N则表示该机器当前不可用
     */
    private Constraint machineModelMatch(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getWorkCenter() != null
                        && !timeslot.getWorkCenter().getId().equals(
                        timeslot.getMaintenance().getWorkCenter().getId()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Machine model must match");
    }


    /**
     * 硬约束2: 机器容量约束
     * 确保每台机器每天的总工作时间不超过其容量,尽量每天留出60分钟剩余容量
     *
     */

    /**
     * 硬约束3: 工序顺序约束
     * 当前工序完成时间必须早于下一道工序开始时间
     * 当存在并行工序时,并序工序开始时间可以一致,但是当工序从并行工序转为串行工序时,其开始时间必须晚于上一道工序的最晚结束时间
     * 同一个订单的同一个任务,每个工序都可以被分为一个或多个时间槽,最小一个(Timeslot),当一个工序被分为一个或多个时间槽时,该工序每天只能被同一个工作中心加工一次
     *
     */
    private Constraint sequentialProcesses(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(t1 -> t1.getStartTime() != null && !CollectionUtils.isEmpty(t1.getProcedure().getNextProcedureNo()))
                .join(Timeslot.class,
                        Joiners.equal(
                                t1 -> t1.getTask().getTaskNo(),
                                t2 -> t2.getTask().getTaskNo()),
                        Joiners.filtering((t1, t2) ->
                                t2.getStartTime() != null
                                        && t1.getProcedure().getNextProcedureNo()
                                        .contains(t2.getProcedure().getProcedureNo())))
                .filter((t1, t2) -> {
                    LocalDateTime t1End = t1.getStartTime().plusMinutes(t1.getDuration());
                    return t1End.isAfter(t2.getStartTime());
                })
                .penalize(HardSoftScore.ONE_HARD, (t1, t2) -> {
                    LocalDateTime t1End = t1.getStartTime().plusMinutes(t1.getDuration());
                    return (int) Duration.between(t2.getStartTime(), t1End).toMinutes();
                })
                .asConstraint("Sequential processes must follow order");
    }

    /**
     * 硬约束5: 订单日期约束
     * 当订单,任务实体中存在factStartDate不为空时,工序中的startTime不为空时,该规划必须按照此订单的实际开始时间为
     * 初始工序的开时间,当工序中的startTime不为空时,此工序必须按照此时间为开始时间,endTime也不为空时则该工序不用继续规划,尊重现实条件
     */


    /**
     * 软约束1: 最小化总完成时间(Makespan)
     * 尽量减少从第一个工序开始到最后一个工序完成的总时间
     */


    /**
     * 软约束2: 鼓励提前开始
     * 订单越早开始越好,特别是高优先级订单
     */

    /**
     * 软约束3: 订单优先级约束
     * 高优先级订单应该在低优先级订单之前完成
     */

    /**
     * 软约束4: 平衡机器负载
     * 尽量让所有机器的工作负载均衡
     */

    /**
     * 软约束5: 最小化同一工序的时间片间隔
     * 同一工序的多个时间片应该连续安排,减少等待时间
     */
}
