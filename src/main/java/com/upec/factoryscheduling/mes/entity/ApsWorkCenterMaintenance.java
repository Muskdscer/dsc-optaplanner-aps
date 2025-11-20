package com.upec.factoryscheduling.mes.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
@Entity
@Table(name = "APS_MACHINE_MAINTENANCE")
public class ApsWorkCenterMaintenance {
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "WORK_CENTER_CODE", nullable = false, length = 20)
    private String workCenterCode;

    @Column(name = "LOCAL_DATE", nullable = false, length = 20)
    private String localDate;

    @Column(name = "CAPACITY", nullable = false)
    private Long capacity;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "START_TIME", nullable = false, length = 20)
    private String startTime;

    @Column(name = "END_TIME", nullable = false, length = 20)
    private String endTime;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    // 手动添加getter和setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkCenterCode() {
        return workCenterCode;
    }

    public void setWorkCenterCode(String workCenterCode) {
        this.workCenterCode = workCenterCode;
    }

    public String getLocalDate() {
        return localDate;
    }

    public void setLocalDate(String localDate) {
        this.localDate = localDate;
    }

    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
