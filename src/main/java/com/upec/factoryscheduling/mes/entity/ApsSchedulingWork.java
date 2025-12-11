package com.upec.factoryscheduling.mes.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "APS_SCHEDULING_WORK")
public class ApsSchedulingWork {
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "SOLVE_ID", length = 30)
    private String solveId;

    @Column(name = "CREATE_TIME", length = 20)
    private LocalDateTime createTime;

    @Column(name = "SOLVE_STATUS", length = 50)
    private String solveStatus;

    @Column(name = "VERSION", length = 20)
    private String version;

}
