package com.upec.factoryscheduling.aps.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Setter
@Getter
@Entity
@Table(name = "aps_orders")
public class Order implements Serializable {

    @Id
    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "erp_status")
    private String erpStatus;

    @Column(name = "order_status")
    private String orderStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "plan_start_date")
    private LocalDate planStartDate;

    @Column(name = "plan_end_date")
    private LocalDate planEndDate;

    @Column(name = "fact_start_date")
    private LocalDateTime factStartDate;

    @Column(name = "fact_end_date")
    private LocalDateTime factEndDate;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "plan_quantity")
    private Integer planQuantity;

    @Column(name = "contract_num")
    private String contractNum;
}
