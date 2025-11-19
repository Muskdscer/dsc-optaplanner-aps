package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工序计划日期变量监听器
 * 
 * 负责根据时间槽的变化动态更新工序的计划开始和结束日期。
 * 
 * 规则：
 * - planStartDate：同Procedure id中所有Timeslot的最小开始时间
 * - planEndDate：同Procedure id中所有Timeslot的最大结束时间
 * 
 * 当Timeslot的startTime或endTime发生变化时，自动更新对应Procedure的计划日期
 */
public class ProcedureVariableListener implements VariableListener<FactorySchedulingSolution, Timeslot> {

    @Override
    public void beforeVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 变量变更前的处理（无需特殊处理）
    }

    @Override
    public void afterVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 当时间槽的开始或结束时间变化时，更新对应工序的计划日期
        if (timeslot.getProcedure() != null) {
            updateProcedurePlanDates(scoreDirector, timeslot.getProcedure());
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加前的处理（无需特殊处理）
    }

    @Override
    public void afterEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 当新的时间槽被添加时，更新对应工序的计划日期
        if (timeslot.getProcedure() != null) {
            updateProcedurePlanDates(scoreDirector, timeslot.getProcedure());
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体移除前的处理（无需特殊处理）
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 当时间槽被移除时，更新对应工序的计划日期
        if (timeslot.getProcedure() != null) {
            updateProcedurePlanDates(scoreDirector, timeslot.getProcedure());
        }
    }

    /**
     * 更新工序的计划开始和结束日期
     */
    private void updateProcedurePlanDates(ScoreDirector<FactorySchedulingSolution> scoreDirector, Procedure procedure) {
        if (procedure == null || procedure.getId() == null) {
            return;
        }

        // 获取同工序ID的所有时间槽
        List<Timeslot> procedureTimeslots = scoreDirector.getWorkingSolution().getTimeslots().stream()
            .filter(timeslot -> timeslot.getProcedure() != null)
            .filter(timeslot -> procedure.getId().equals(timeslot.getProcedure().getId()))
            .filter(timeslot -> timeslot.getStartTime() != null && timeslot.getEndTime() != null)
            .collect(Collectors.toList());

        if (procedureTimeslots.isEmpty()) {
            // 没有时间槽，清除计划日期
            updatePlanDates(scoreDirector, procedure, null, null);
            return;
        }

        // 计算最小开始时间和最大结束时间
        LocalDateTime minStartTime = procedureTimeslots.stream()
            .map(Timeslot::getStartTime)
            .min(LocalDateTime::compareTo)
            .orElse(null);

        LocalDateTime maxEndTime = procedureTimeslots.stream()
            .map(Timeslot::getEndTime)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        // 转换为LocalDate（只取日期部分）
        LocalDate planStartDate = minStartTime != null ? minStartTime.toLocalDate() : null;
        LocalDate planEndDate = maxEndTime != null ? maxEndTime.toLocalDate() : null;

        // 更新工序的计划日期
        updatePlanDates(scoreDirector, procedure, planStartDate, planEndDate);

        System.out.println("更新工序 " + procedure.getId() + " 的计划日期: " +
                          "planStartDate=" + planStartDate + ", planEndDate=" + planEndDate +
                          " (基于 " + procedureTimeslots.size() + " 个时间槽)");
    }

    /**
     * 更新工序的计划日期
     */
    private void updatePlanDates(ScoreDirector<FactorySchedulingSolution> scoreDirector, 
                               Procedure procedure, LocalDate planStartDate, LocalDate planEndDate) {
        scoreDirector.beforeVariableChanged(procedure, "planStartDate");
        procedure.setPlanStartDate(planStartDate);
        scoreDirector.afterVariableChanged(procedure, "planStartDate");

        scoreDirector.beforeVariableChanged(procedure, "planEndDate");
        procedure.setPlanEndDate(planEndDate);
        scoreDirector.afterVariableChanged(procedure, "planEndDate");
    }
}
