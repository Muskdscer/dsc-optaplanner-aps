package com.upec.factoryscheduling.aps.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")

@Setter
@Getter
public class Order {

    @Id
    private String orderNo;

    private String erpStatus;

    private String orderStatus;

    private LocalDate planStartDate;

    private LocalDate planEndDate;

    private LocalDateTime factStartDate;

    private LocalDateTime factEndDate;
}
