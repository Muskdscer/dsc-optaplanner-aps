package com.upec.factoryscheduling.aps.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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

    private int machineMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Integer> nextProcedureNo;

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Procedure> nextProcedure;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDate planStartDate;

    private LocalDate planEndDate;

    private String status;

    private boolean parallel;

    private int index;

    private Integer level;

    public void addNextProcedure(Procedure procedure) {
        if (this.nextProcedure == null) {
            this.nextProcedure = new ArrayList<>();
        }
        this.nextProcedure.add(procedure);
    }

}
