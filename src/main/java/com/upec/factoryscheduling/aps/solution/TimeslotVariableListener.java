package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

@Slf4j
public class TimeslotVariableListener implements VariableListener<FactorySchedulingSolution, Timeslot>, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 全局锁 - 用于保护关键操作，使用StampedLock提高读写性能
     */
    private final StampedLock globalLock = new StampedLock();

    @Override
    public void beforeVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 变量变更前不需要特殊处理
        if (timeslot.getMaintenance() != null) {
//            releaseStartTimeAndEndTime(scoreDirector, timeslot);
        }
    }

    @Override
    public void afterVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 当maintenance变量变更时，更新时间槽的开始和结束时间
        if (timeslot.getMaintenance() != null) {
            updateStartTimeAndEndTime(scoreDirector, timeslot);
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加前不需要特殊处理
        if (timeslot.getMaintenance() != null) {
            log.info("beforeEntityAdded timeslot :{}", timeslot.getMaintenance().getId());
        }
    }

    @Override
    public void afterEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 当实体被添加时，如果已分配维护计划，则更新时间
        if (timeslot.getMaintenance() != null) {
            log.info("afterEntityAdded timeslot :{}", timeslot.getMaintenance().getId());
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体移除前不需要特殊处理
        if (timeslot.getMaintenance() != null) {
            log.info("beforeEntityRemoved timeslot :{}", timeslot.getMaintenance().getId());
        }
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体移除后，可能需要重新计算相关时间槽的时间
        if (timeslot.getMaintenance() != null) {
            log.info("afterEntityRemoved timeslot :{}", timeslot.getMaintenance().getId());
        }
    }

    /**
     * 更新时间槽的开始时间和结束时间
     * 在多线程环境中确保线程安全
     */
    private synchronized void updateStartTimeAndEndTime(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 防御性检查
        if (timeslot == null || timeslot.getMaintenance() == null || timeslot.getMaintenance().getWorkCenter() == null ||
                timeslot.getMaintenance().getDate() == null) {
            return;
        }
        // 获取时间槽的锁，确保线程安全的操作
        ReentrantLock timeslotLock = timeslot.getLock();
        long stamp = globalLock.writeLock();
        timeslotLock.lock();
        try {
            WorkCenterMaintenance maintenance = timeslot.getMaintenance();
            // 使用ScoreDirector通知变量变更
            scoreDirector.beforeVariableChanged(timeslot, "startTime");
            scoreDirector.beforeVariableChanged(timeslot, "endTime");
            // 使用线程安全的方式更新时间槽的时间
            timeslot.updateTimeRange();
            // 通知ScoreDirector变量已变更
            scoreDirector.afterVariableChanged(timeslot, "startTime");
            scoreDirector.afterVariableChanged(timeslot, "endTime");
        } catch (Exception e) {
            // 记录异常但不中断处理
            log.error("Error updating timeslot time range: {}", e.getMessage(), e);
        } finally {
            timeslotLock.unlock();
            globalLock.unlockWrite(stamp);
        }
    }


    private synchronized void releaseStartTimeAndEndTime(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 防御性检查
        if (timeslot == null || timeslot.getMaintenance() == null) {
            return;
        }
        // 获取时间槽的锁，确保线程安全的操作
        ReentrantLock timeslotLock = timeslot.getLock();
        long stamp = globalLock.writeLock();
        timeslotLock.lock();
        try {
            WorkCenterMaintenance maintenance = timeslot.getMaintenance();
            // 使用ScoreDirector通知变量变更
            scoreDirector.beforeVariableChanged(timeslot, "startTime");
            scoreDirector.beforeVariableChanged(timeslot, "endTime");
            // 使用线程安全的方式更新时间槽的时间
            timeslot.releaseTimeRange();
            // 通知ScoreDirector变量已变更
            scoreDirector.afterVariableChanged(timeslot, "startTime");
            scoreDirector.afterVariableChanged(timeslot, "endTime");
        } catch (Exception e) {
            // 记录异常但不中断处理
            log.error("Error updating timeslot time range: {}", e.getMessage(), e);
        } finally {
            timeslotLock.unlock();
            globalLock.unlockWrite(stamp);
        }
    }
}
