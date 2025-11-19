package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.lookup.PlanningId;

import javax.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.optaplanner.core.api.domain.entity.PlanningEntity;

@Entity
@Table(name = "work_center_maintenance")
@PlanningEntity
@Data
@Getter
@Setter
public class WorkCenterMaintenance {

    @Id
    @PlanningId
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenter workCenter;

    private int year;

    private LocalDate date;

    private BigDecimal capacity;

    private String status;

    private String description;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal usageTime;

    /**
     * 检查是否还有可用容量
     */
    public boolean hasAvailableCapacity() {
        BigDecimal capacity = this.capacity != null ? this.capacity : BigDecimal.ZERO;
        BigDecimal usage = this.usageTime != null ? this.usageTime : BigDecimal.ZERO;
        return usage.compareTo(capacity) < 0;
    }

    /**
     * 获取剩余可用容量
     */
    public BigDecimal getRemainingCapacity() {
        BigDecimal capacity = this.capacity != null ? this.capacity : BigDecimal.ZERO;
        BigDecimal usage = this.usageTime != null ? this.usageTime : BigDecimal.ZERO;
        return capacity.subtract(usage);
    }

    /**
     * 累加使用时间
     */
    public void addUsageTime(BigDecimal duration) {
        if (this.usageTime == null) {
            this.usageTime = BigDecimal.ZERO;
        }
        if (duration != null) {
            this.usageTime = this.usageTime.add(duration);
        }
    }

    /**
     * 减少使用时间
     */
    public void subtractUsageTime(BigDecimal duration) {
        if (this.usageTime != null && duration != null) {
            this.usageTime = this.usageTime.subtract(duration);
            if (this.usageTime.compareTo(BigDecimal.ZERO) < 0) {
                this.usageTime = BigDecimal.ZERO;
            }
        }
    }

    public WorkCenterMaintenance() {
    }

    public WorkCenterMaintenance(WorkCenter workCenter, LocalDate date,  BigDecimal capacity, String description) {
        this.workCenter = workCenter;
        this.date = date;
        this.capacity = capacity;
        this.description = description;
    }

}
