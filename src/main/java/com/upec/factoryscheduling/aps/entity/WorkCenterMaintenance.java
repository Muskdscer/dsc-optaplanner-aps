package com.upec.factoryscheduling.aps.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "aps_work_center_maintenance")
@Getter
@Setter
public class WorkCenterMaintenance implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_center")
    private WorkCenter workCenter;

    @Column(name = "calendar_year")
    private int year;

    @Column(name = "calendar_date")
    private LocalDate date;

    //机器容量(分钟)
    private int capacity;

    private String status;

    private String description;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "usage_time")
    private int usageTime;

    /**
     * 检查是否还有可用容量
     */
    public synchronized boolean hasAvailableCapacity() {
        return this.capacity >= this.usageTime;
    }

    /**
     * 获取剩余可用容量
     */
    public synchronized int getRemainingCapacity() {
        return this.capacity - this.usageTime;
    }

    /**
     * 累加使用时间 - 线程安全
     */
    public synchronized void addUsageTime(int duration) {
        this.usageTime = this.usageTime + duration;
    }

    /**
     * 减少使用时间 - 线程安全
     */
    public synchronized void subtractUsageTime(int duration) {
        this.usageTime = this.usageTime - duration;
    }

    public WorkCenterMaintenance() {
    }

    public WorkCenterMaintenance(WorkCenter workCenter, LocalDate date, int capacity, String description) {
        this.workCenter = workCenter;
        this.date = date;
        this.capacity = capacity;
        this.description = description;
    }

}
