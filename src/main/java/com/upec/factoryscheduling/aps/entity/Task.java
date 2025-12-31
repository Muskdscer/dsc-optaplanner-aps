package com.upec.factoryscheduling.aps.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "aps_task")
public class Task implements Serializable {

    @Id
    @Column(name = "task_no")
    private String taskNo;

    @Column(name = "order_no")
    private String orderNo;

    private String status;

    private Integer planQuantity;

    //实际开始时间
    @Column(name = "fact_start_date")
    private LocalDateTime factStartDate;

    //实际结束时间
    @Column(name = "fact_end_date")
    private LocalDateTime factEndDate;

    //计划开始时间
    @Column(name = "plan_start_date")
    private LocalDate planStartDate;

    //计划结束时间
    @Column(name = "plan_end_date")
    private LocalDate planEndDate;

    private int priority;

    private String routeId;

    @Column(name = "LOCKED_REMARK",length = 2000)
    private String lockedRemark;

    private LocalDateTime createDate;

}
