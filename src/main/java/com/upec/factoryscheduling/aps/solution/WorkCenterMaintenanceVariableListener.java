package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作中心维护计划变量监听器
 * 
 * 负责根据维护计划的变化自动更新时间槽的开始时间，
 * 并管理工作中心的使用容量（usageTime）。
 * 
 * 当Timeslot的maintenance变量发生变化时：
 * 1. 根据维护计划的日期和开始时间计算时间槽的开始时间
 * 2. 累加或释放工作中心的使用容量
 * 3. 确保容量不超过限制（capacity）
 */
public class WorkCenterMaintenanceVariableListener implements VariableListener<FactorySchedulingSolution, Timeslot> {

    @Override
    public void beforeVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 变量变更前的处理
        if (timeslot.getMaintenance() != null && timeslot.getDuration() != null) {
            // 释放之前占用的容量
            releaseCapacity(scoreDirector, timeslot.getMaintenance(), timeslot.getDuration());
        }
    }

    @Override
    public void afterVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 变量变更后的处理
        if (timeslot.getMaintenance() != null) {
            updateTimeslotStartTime(scoreDirector, timeslot);
            
            if (timeslot.getDuration() != null) {
                // 分配新的容量
                allocateCapacity(scoreDirector, timeslot, timeslot.getMaintenance(), timeslot.getDuration());
            }
        } else {
            // 如果没有维护计划，清除开始时间
            scoreDirector.beforeVariableChanged(timeslot, "startTime");
            timeslot.setStartTime(null);
            scoreDirector.afterVariableChanged(timeslot, "startTime");
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加前的处理
    }

    @Override
    public void afterEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加后的处理
        if (timeslot.getMaintenance() != null) {
            updateTimeslotStartTime(scoreDirector, timeslot);
            
            if (timeslot.getDuration() != null) {
                allocateCapacity(scoreDirector, timeslot, timeslot.getMaintenance(), timeslot.getDuration());
            }
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体移除前的处理
        if (timeslot.getMaintenance() != null && timeslot.getDuration() != null) {
            releaseCapacity(scoreDirector, timeslot.getMaintenance(), timeslot.getDuration());
        }
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体移除后的处理
    }

    /**
     * 根据维护计划更新时间槽的开始时间
     */
    private void updateTimeslotStartTime(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        WorkCenterMaintenance maintenance = timeslot.getMaintenance();
        if (maintenance == null || maintenance.getDate() == null || maintenance.getStartTime() == null) {
            return;
        }

        // 计算开始时间：维护计划日期 + 开始时间
        LocalDateTime startTime = LocalDateTime.of(maintenance.getDate(), maintenance.getStartTime());
        
        scoreDirector.beforeVariableChanged(timeslot, "startTime");
        timeslot.setStartTime(startTime);
        scoreDirector.afterVariableChanged(timeslot, "startTime");
    }

    /**
     * 分配工作中心容量
     */
    private void allocateCapacity(ScoreDirector<FactorySchedulingSolution> scoreDirector, 
                                  Timeslot timeslot, WorkCenterMaintenance maintenance, BigDecimal duration) {
        if (maintenance == null || duration == null) {
            return;
        }

        // 检查是否有足够容量
        if (!maintenance.hasAvailableCapacity()) {
            System.out.println("警告：工作中心 " + maintenance.getWorkCenter().getId() + 
                             " 在日期 " + maintenance.getDate() + " 容量不足");
            return;
        }

        // 检查是否会超出容量限制
        BigDecimal remainingCapacity = maintenance.getRemainingCapacity();
        if (duration.compareTo(remainingCapacity) > 0) {
            System.out.println("警告：工作中心 " + maintenance.getWorkCenter().getId() + 
                             " 在日期 " + maintenance.getDate() + " 剩余容量不足，需要 " + duration + 
                             " 但只剩 " + remainingCapacity);
            return;
        }

        // 累加使用时间
        scoreDirector.beforeVariableChanged(maintenance, "usageTime");
        maintenance.addUsageTime(duration);
        scoreDirector.afterVariableChanged(maintenance, "usageTime");
        
        System.out.println("工作中心 " + maintenance.getWorkCenter().getId() + 
                          " 在日期 " + maintenance.getDate() + " 分配容量 " + duration + 
                          " 小时，当前使用容量：" + maintenance.getUsageTime() + "/" + maintenance.getCapacity());
    }

    /**
     * 释放工作中心容量
     */
    private void releaseCapacity(ScoreDirector<FactorySchedulingSolution> scoreDirector, WorkCenterMaintenance maintenance, BigDecimal duration) {
        if (maintenance == null || duration == null) {
            return;
        }

        // 减少使用时间
        scoreDirector.beforeVariableChanged(maintenance, "usageTime");
        maintenance.subtractUsageTime(duration);
        scoreDirector.afterVariableChanged(maintenance, "usageTime");
        
        System.out.println("工作中心 " + maintenance.getWorkCenter().getId() + 
                          " 在日期 " + maintenance.getDate() + " 释放容量 " + duration + 
                          " 小时，当前使用容量：" + maintenance.getUsageTime() + "/" + maintenance.getCapacity());
    }
}
