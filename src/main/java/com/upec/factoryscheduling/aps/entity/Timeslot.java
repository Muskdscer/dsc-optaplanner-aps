package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.aps.solution.TimeslotVariableListener;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Entity
@PlanningEntity
@Getter
@Setter
@Data
public class Timeslot implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * -- GETTER --
     * 获取锁对象，用于复杂的原子操作
     *
     * @return 锁对象
     */
    private final transient ReentrantLock lock = new ReentrantLock();

    @Id
    @PlanningId
    private volatile String id;

    private volatile Long problemId;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private volatile Procedure procedure;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private volatile Order order;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private volatile Task task;

    @OneToOne(fetch = FetchType.EAGER)
    private volatile WorkCenter workCenter;

    private volatile int duration;

    private volatile Integer priority;

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName =
            "maintenance", sourceEntityClass = Timeslot.class)
    private volatile LocalDateTime startTime;

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "maintenance",
            sourceEntityClass = Timeslot.class)
    private volatile LocalDateTime endTime;

    @PlanningVariable(valueRangeProviderRefs = "maintenanceRange")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private volatile WorkCenterMaintenance maintenance;

    private volatile boolean parallel;

    private volatile boolean manual;

    private volatile int index;

    private volatile int total;

    private volatile int procedureIndex;

    /**
     * 线程安全地设置ID
     *
     * @param id 新的ID值
     */
    public synchronized void setId(String id) {
        this.id = id;
    }

    /**
     * 线程安全地设置问题ID
     *
     * @param problemId 新的问题ID
     */
    public synchronized void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    /**
     * 线程安全地设置工序
     *
     * @param procedure 新的工序
     */
    public synchronized void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

    /**
     * 线程安全地设置订单
     *
     * @param order 新的订单
     */
    public synchronized void setOrder(Order order) {
        this.order = order;
    }

    /**
     * 线程安全地设置任务
     *
     * @param task 新的任务
     */
    public synchronized void setTask(Task task) {
        this.task = task;
    }

    /**
     * 线程安全地设置工作中心
     *
     * @param workCenter 新的工作中心
     */
    public synchronized void setWorkCenter(WorkCenter workCenter) {
        this.workCenter = workCenter;
    }

    /**
     * 线程安全地设置持续时间
     *
     * @param duration 新的持续时间
     */
    public synchronized void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * 线程安全地设置优先级
     *
     * @param priority 新的优先级
     */
    public synchronized void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 线程安全地设置开始时间
     *
     * @param startTime 新的开始时间
     */
    public synchronized void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 线程安全地设置结束时间
     *
     * @param endTime 新的结束时间
     */
    public synchronized void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 线程安全地设置维护计划
     *
     * @param maintenance 新的维护计划
     */
    public synchronized void setMaintenance(WorkCenterMaintenance maintenance) {
        this.maintenance = maintenance;
    }

    /**
     * 线程安全地设置并行标志
     *
     * @param parallel 新的并行状态
     */
    public synchronized void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * 线程安全地设置手动标志
     *
     * @param manual 新的手动状态
     */
    public synchronized void setManual(boolean manual) {
        this.manual = manual;
    }

    /**
     * 线程安全地设置索引
     *
     * @param index 新的索引值
     */
    public synchronized void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * 线程安全地设置总分片数量
     *
     * @param total 新的总分片数量
     */
    public synchronized void setTotal(Integer total) {
        this.total = total;
    }

    /**
     * 线程安全地设置工序索引
     *
     * @param procedureIndex 新的工序索引
     */
    public synchronized void setProcedureIndex(int procedureIndex) {
        this.procedureIndex = procedureIndex;
    }


    public synchronized void updateTimeRange() {

        lock.lock();
        try {
            if (maintenance == null|| workCenter == null) {
                return;
            }
            if (Objects.equals(this.maintenance.getWorkCenter().getWorkCenterCode(), this.workCenter.getWorkCenterCode())) {
                this.startTime = maintenance.getDate().atTime(maintenance.getStartTime());
                this.endTime = this.startTime.plusMinutes(this.duration);
                this.maintenance.addUsageTime(this.duration);
            }
        } finally {
            lock.unlock();
        }
    }

    public synchronized void releaseTimeRange() {
        lock.lock();
        try {
            if (maintenance == null|| workCenter == null) {
                return;
            }
            if (this.maintenance.getWorkCenter().getWorkCenterCode().equals(this.workCenter.getWorkCenterCode())) {
                this.startTime = null;
                this.endTime = null;
                this.maintenance.subtractUsageTime(this.duration);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 线程安全地检查时间重叠
     *
     * @param other 另一个时间槽
     * @return 是否存在重叠
     */
    public boolean overlapsWith(Timeslot other) {
        if (other == null || workCenter == null || other.getWorkCenter() == null) {
            return false;
        }

        boolean workCentersMatch = workCenter.equals(other.getWorkCenter());

        if (!workCentersMatch) {
            return false;
        }

        lock.lock();
        try {
            LocalDateTime thisStart = startTime;
            LocalDateTime thisEnd = endTime;
            LocalDateTime otherStart = other.getStartTime();
            LocalDateTime otherEnd = other.getEndTime();

            // 两个时间槽都有开始和结束时间才能检查重叠
            if (thisStart == null || thisEnd == null || otherStart == null || otherEnd == null) {
                return false;
            }

            // 检查是否有重叠
            return !thisEnd.isBefore(otherStart) && !thisStart.isAfter(otherEnd);
        } finally {
            lock.unlock();
        }
    }

}
