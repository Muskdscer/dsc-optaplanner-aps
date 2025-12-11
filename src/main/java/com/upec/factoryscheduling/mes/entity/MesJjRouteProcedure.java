package com.upec.factoryscheduling.mes.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
@Data
@Setter
@Getter
@Entity
@Table(name = "MES_JJ_ROUTE_PROCEDURE")
public class MesJjRouteProcedure implements Serializable {
    @Id
    @Column(name = "SEQ", nullable = false, length = 20)
    private String seq;

    @Column(name = "ROUTE_SEQ", length = 20)
    private String routeSeq;

    @Column(name = "PROCEDURENO", length = 20)
    private String procedureNo;

    @Column(name = "PROCEDURE_CODE", length = 20)
    private String procedureCode;

    @Column(name = "PROCEDURE_NAME", length = 100)
    private String procedureName;

    @Column(name = "NEXT_PROCEDURENO", length = 20)
    private String nextProcedureNo;

    @Column(name = "PROCEDURE_TYPE", length = 200)
    private String procedureType;

    @Column(name = "PROCEDURE_CONTENT", length = 1000)
    private String procedureContent;

    @Column(name = "WORKCENTER_SEQ", length = 20)
    private String workCenterSeq;

    @Column(name = "MACHINE_HOURS", length = 10)
    private String machineHours;

    @Column(name = "HUMAN_HOURS", length = 10)
    private String humanHours;

    @Column(name = "PROGRAM_NAME", length = 100)
    private String programName;

    @Column(name = "DUTYUSER", length = 20)
    private String dutyUser;

    @Column(name = "REMARK", length = 200)
    private String remark;

    @Column(name = "CREATEUSER", length = 20)
    private String createUser;

    @Column(name = "CREATEDATE", length = 20)
    private String createDate;

    @Column(name = "UPDATEUSER", length = 20)
    private String updateUser;

    @Column(name = "UPDATEDATE", length = 20)
    private String updateDate;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "RETURNMSG", length = 500)
    private String returnMsg;

    @Column(name = "SUBMITUSER", length = 20)
    private String submitUser;

    @Column(name = "SUBMITDATE", length = 20)
    private String submitDate;

    @Column(name = "KEY_INDICATORS", length = 1000)
    private String keyIndicators;

    @Column(name = "QUANTITY", length = 20)
    private String quantity;

}
