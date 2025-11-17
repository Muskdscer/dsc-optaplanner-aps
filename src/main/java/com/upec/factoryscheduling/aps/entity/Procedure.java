package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@Entity
@Table(name = "procedure")
public class Procedure implements Serializable {

    @Id
    private String id;

    private String taskNo;

    private String orderNo;

    @OneToOne
    private WorkCenter workCenterId;

    private String procedureName;

    private Integer procedureNo;

    private double machineHours;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Integer> nextProcedureNo;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Procedure> nextProcedure;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDate planStartDate;

    private LocalDate planEndDate;

    private String status;

    private boolean parallel;

}
