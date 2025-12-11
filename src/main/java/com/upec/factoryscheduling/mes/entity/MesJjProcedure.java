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
@Table(name = "MES_JJ_PROCEDURE")
public class MesJjProcedure {
    @Id
    @Column(name = "SEQ", nullable = false, length = 20)
    private String seq;

    @Column(name = "ORDERNO", length = 20)
    private String orderNo;

    @Column(name = "TASKNO", length = 20)
    private String taskNo;

    @Column(name = "PROCEDURENO", length = 20)
    private String procedureNo;

    @Column(name = "PROCEDURE_NAME", length = 2000)
    private String procedureName;

    @Column(name = "NEXT_PROCEDURENO", length = 20)
    private String nextProcedureNo;

    @Column(name = "PROCEDURE_TYPE", length = 10)
    private String procedureType;

    @Column(name = "PRDMANAGER_SEQ", length = 20)
    private String prdManagerSeq;

    @Column(name = "WORKCENTER_CODE", length = 20)
    private String workCenterSeq;

    @Column(name = "PREPARE_HOURS", length = 10)
    private String prepareHours;

    @Column(name = "MACHINE_HOURS", length = 10)
    private String machineHours;

    @Column(name = "HUMAN_HOURS", length = 10)
    private String humanHours;

    @Column(name = "PROCEDURE_STATUS", length = 20)
    private String procedureStatus;

    @Column(name = "FACT_STARTDATE", length = 20)
    private String factStartDate;

    @Column(name = "FACT_ENDDATE", length = 20)
    private String factEndDate;

    @Column(name = "QUALITY_USER", length = 1000)
    private String qualityUser;

    @Column(name = "REWORK_FLAG", length = 1)
    private String reworkFlag;

    @Column(name = "ASSIST_PROCESSINSTANCE", length = 40)
    private String assistProcessInstance;

    @Column(name = "ASSIST_PRDMANAGER_SEQ", length = 20)
    private String assistPrdManagerSeq;

    @Column(name = "CREATEUSER", length = 20)
    private String createUser;

    @Column(name = "CREATEDATE", length = 20)
    private String createdate;

    @Column(name = "PRODURE_HOURS", length = 20)
    private String produreHours;

    @Column(name = "ERP_PROCEDURENO", length = 20)
    private String erpProcedureNo;

    @Column(name = "PLAN_STARTDATE", length = 20)
    private String planStartDate;

    @Column(name = "PLAN_ENDDATE", length = 20)
    private String planEndDate;

    @Column(name = "UNQUALIFIED_PROCESSINSTANCE", length = 200)
    private String unqualifiedProcessInstance;

    @Column(name = "SELF_CHECK_RESULT", length = 20)
    private String selfCheckResult;

    @Column(name = "SELF_CHECK_REMARK", length = 2000)
    private String selfCheckRemark;

    @Column(name = "ROUTE_SEQ", length = 20)
    private String routeSeq;

    @Column(name = "UPDATEUSER", length = 20)
    private String updateUser;

    @Column(name = "UPDATEDATE", length = 20)
    private String updateDate;

    @Column(name = "MAKEDNUMBER", length = 20)
    private String makedNumber;

    @Column(name = "QUICKPROCESSINSTANCE", length = 50)
    private String quickProcessInstance;

}
