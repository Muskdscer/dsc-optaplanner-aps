package com.upec.factoryscheduling.aps.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "task")
public class Task implements Serializable {

    @Id
    private String taskNo;

    private String orderNo;

    private String status;

    //实际开始时间
    private LocalDateTime factStartDate;

    //实际结束时间
    private LocalDateTime factEndDate;

    //计划开始时间
    private LocalDate planStartDate;

    //计划结束时间
    private LocalDate planEndDate;

    private int priority;
}
