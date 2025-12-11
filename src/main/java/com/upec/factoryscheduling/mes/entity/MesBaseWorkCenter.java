package com.upec.factoryscheduling.mes.entity;

import javax.persistence.*;
@Entity
@Table(name = "MES_BASE_WORKCENTER",  indexes = {
        @Index(name = "IDX$$_03B60003", columnList = "WORKCENTER_CODE")
})
public class MesBaseWorkCenter {
    @Id
    @Column(name = "SEQ", nullable = false, length = 20)
    private String seq;

    @Column(name = "WORKCENTER_CODE", length = 20)
    private String workCenterCode;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "COSTCENTER_SEQ", length = 20)
    private String costCenterSeq;

    @Column(name = "STATUS", length = 1)
    private String status;

    @Column(name = "FACTORY_SEQ", length = 20)
    private String factorySeq;

    @Column(name = "CREATEUSER", length = 20)
    private String createUser;

    @Column(name = "CREATEDATE", length = 20)
    private String createDate;

    @Column(name = "ATTRIBUTES", length = 8)
    private String attributes;

    @Column(name = "MACHINE_HOURS_COST", length = 10)
    private String machineHoursCost;

    @Column(name = "HUMAN_HOURS_COST", length = 10)
    private String humanHoursCost;

    @Column(name = "WORK_CENTER_GROUP", length = 50)
    private String workCenterGroup;

    @Column(name = "REMARK", length = 200)
    private String remark;

    // 手动添加getter和setter方法
    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public String getWorkCenterCode() {
        return workCenterCode;
    }

    public void setWorkCenterCode(String workCenterCode) {
        this.workCenterCode = workCenterCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCostCenterSeq() {
        return costCenterSeq;
    }

    public void setCostCenterSeq(String costCenterSeq) {
        this.costCenterSeq = costCenterSeq;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFactorySeq() {
        return factorySeq;
    }

    public void setFactorySeq(String factorySeq) {
        this.factorySeq = factorySeq;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getMachineHoursCost() {
        return machineHoursCost;
    }

    public void setMachineHoursCost(String machineHoursCost) {
        this.machineHoursCost = machineHoursCost;
    }

    public String getHumanHoursCost() {
        return humanHoursCost;
    }

    public void setHumanHoursCost(String humanHoursCost) {
        this.humanHoursCost = humanHoursCost;
    }

    public String getWorkCenterGroup() {
        return workCenterGroup;
    }

    public void setWorkCenterGroup(String workCenterGroup) {
        this.workCenterGroup = workCenterGroup;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}