package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

@Slf4j
public class TimeslotVariableListener implements VariableListener<FactorySchedulingSolution, Timeslot>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String WORK_CENTER_CODE = "PM10W200";

    /**
     * 全局锁 - 用于保护关键操作，使用StampedLock提高读写性能
     */
    private final StampedLock globalLock = new StampedLock();

    @Override
    public void beforeVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 变量变更前不需要特殊处理
    }

    @Override
    public void afterVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        Procedure procedure = timeslot.getProcedure();
        if (procedure.getWorkCenterId() != null && procedure.getWorkCenterId().getWorkCenterCode().equals(WORK_CENTER_CODE)) {

        }
        // 当maintenance或duration变量变更时，更新时间槽的开始和结束时间
        if (timeslot.getMaintenance() != null) {
            WorkCenterMaintenance maintenance = timeslot.getMaintenance();
            // 使用ScoreDirector通知变量变更
            scoreDirector.beforeVariableChanged(timeslot, "startTime");
            // 使用线程安全的方式更新时间槽的时间
            LocalDateTime startTime = maintenance.getDate().atTime(maintenance.getStartTime());
            timeslot.setStartTime(startTime);
            // 通知ScoreDirector变量已变更
            scoreDirector.afterVariableChanged(timeslot, "startTime");
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

    private void setOutsourcingTime(Timeslot timeslot) {
        Procedure procedure = timeslot.getProcedure();
        List<Procedure> procedures = procedure.getNextProcedure();

    }
}
