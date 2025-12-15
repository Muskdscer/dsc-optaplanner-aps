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
import java.time.Duration;
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

    private final transient ReentrantLock lock = new ReentrantLock();

    @Id
    @PlanningId
    private String id;

    private Long problemId;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Procedure procedure;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Order order;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Task task;

    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenter workCenter;

    //该时间槽当天所需分配时间(分钟)
    private int duration;

    //优先级
    private Integer priority;

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName =
            "maintenance", sourceEntityClass = Timeslot.class)
    //规划开始时间
    private LocalDateTime startTime;

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "maintenance",
            sourceEntityClass = Timeslot.class)
    //规划结束时间
    private LocalDateTime endTime;

    @PlanningVariable(valueRangeProviderRefs = "maintenanceRange")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    //绑定的工作中心日历
    private WorkCenterMaintenance maintenance;

    //当前工序是否为并行工序
    private boolean parallel;

    //当天工序已完成或者手动排序,该时间槽不可动
    private boolean manual;

    //当前工序的时间槽索引
    private int index;

    //当前工序的总时间槽个数
    private int total;

    //当前工序索引
    private int procedureIndex;

    public synchronized void updateTimeRange() {
        lock.lock();
        try {
            if (maintenance == null || workCenter == null) {
                return;
            }
            if (Objects.equals(this.maintenance.getWorkCenter().getWorkCenterCode(), this.workCenter.getWorkCenterCode())) {
                this.startTime = maintenance.getDate().atTime(maintenance.getStartTime());
                this.endTime = this.startTime.plusMinutes(this.duration);
            }
        } finally {
            lock.unlock();
        }
    }

    public synchronized void releaseTimeRange() {
        lock.lock();
        try {
            if (maintenance == null || workCenter == null) {
                return;
            }
            if (this.maintenance.getWorkCenter().getWorkCenterCode().equals(this.workCenter.getWorkCenterCode())) {
                this.startTime = null;
                this.endTime = null;
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



    /**
     * 优化的重叠检查方法
     */
    public boolean overlapsWithOptimized(Timeslot other) {
        if (other == null ||
                workCenter == null ||
                other.getWorkCenter() == null ||
                startTime == null ||
                endTime == null ||
                other.getStartTime() == null ||
                other.getEndTime() == null) {
            return false;
        }

        // 快速检查：工作中心不同则不重叠
        if (!workCenter.getId().equals(other.getWorkCenter().getId())) {
            return false;
        }

        // 使用时间比较而不是Duration计算，性能更好
        return !(endTime.isBefore(other.getStartTime()) ||
                startTime.isAfter(other.getEndTime()));
    }
    /**
     * 计算与另一个时间槽的重叠分钟数
     */
    public long calculateOverlapMinutes(Timeslot other) {
        if (!overlapsWithOptimized(other)) {
            return 0;
        }

        LocalDateTime overlapStart = startTime.isAfter(other.getStartTime()) ?
                startTime : other.getStartTime();
        LocalDateTime overlapEnd = endTime.isBefore(other.getEndTime()) ?
                endTime : other.getEndTime();

        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }

}
