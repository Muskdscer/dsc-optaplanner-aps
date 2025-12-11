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
@Table(name = "MES_JJ_ORDER")
public class MesJjOrder {
    @Id
    @Column(name = "ORDERNO", nullable = false, length = 20)
    private String orderNo;

    @Column(name = "PLAN_QUANTITY", length = 10)
    private String planQuantity;

    @Column(name = "FACTORY_CODE", length = 10)
    private String factoryCode;

    @Column(name = "PRDMANAGER_SEQ", length = 20)
    private String prdManagerSeq;

    @Column(name = "ORDER_TYPE", length = 10)
    private String orderType;

    @Column(name = "ERP_STATUS", length = 10)
    private String erpStatus;

    @Column(name = "ORDER_STATUS", length = 10)
    private String orderStatus;

    @Column(name = "PLAN_STARTDATE", length = 20)
    private String planStartDate;

    @Column(name = "PLAN_ENDDATE", length = 20)
    private String planEndDate;

    @Column(name = "FACT_STARTDATE", length = 20)
    private String factStartDate;

    @Column(name = "FACT_ENDDATE", length = 20)
    private String factEndDate;

    @Column(name = "FACT_QUANTITY", length = 10)
    private String factQuantity;

    @Column(name = "CREATEUSER", length = 20)
    private String createUser;

    @Column(name = "CREATEDATE", length = 20)
    private String createDate;

    @Column(name = "CONTRACTNUM", length = 50)
    private String contractNum;

    public String getOrderNo() {
        return orderNo;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getErpStatus() {
        return erpStatus;
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