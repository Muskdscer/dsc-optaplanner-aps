package com.upec.factoryscheduling.mes.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "MES_JJ_ORDER_TASK")
public class MesJjOrderTask {
    @Id
    @Column(name = "TASKNO", nullable = false, length = 20)
    private String taskNo;

    @Column(name = "ORDERNO", length = 20)
    private String orderNo;

    @Column(name = "ROUTE_SEQ", length = 20)
    private String routeSeq;

    @Column(name = "PLAN_QUANTITY", length = 10)
    private String planQuantity;

    @Column(name = "TASK_STATUS", length = 30)
    private String taskStatus;

    @Column(name = "FACT_STARTDATE", length = 20)
    private String factStartDate;

    @Column(name = "FACT_ENDDATE", length = 20)
    private String factEndDate;

    @Column(name = "CREATEUSER", length = 20)
    private String createUser;

    @Column(name = "CREATEDATE", length = 20)
    private String createDate;

    @Column(name = "PLAN_STARTDATE", length = 20)
    private String planStartDate;

    @Column(name = "PLAN_ENDDATE", length = 20)
    private String planEndDate;

    @Column(name = "OLD_TASKNO", length = 100)
    private String oldTaskNo;

    @Column(name = "LOCKEDUSER", length = 200)
    private String lockedUser;

    @Column(name = "LOCKEDDATE", length = 200)
    private String lockedDate;

    @Column(name = "BEFORE_TASKSTATUS", length = 200)
    private String beforeTaskStatus;

    @Column(name = "LOCKEDREMARK", length = 2000)
    private String lockedRemark;

    @Column(name = "MARK", length = 20)
    private String mark;

    public String getTaskNo() {
        return taskNo;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getPlanStartDate() {
        return planStartDate;
    }

    public String getPlanEndDate() {
        return planEndDate;
    }

    public String getFactStartDate() {
        return factStartDate;
    }

    public String getFactEndDate() {
        return factEndDate;
    }
}