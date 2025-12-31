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
@Table(name = "aps_procedure")
public class Procedure implements Serializable {

    @Id
    private String id;

    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name ="order_no" )
    private Order order;

    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "task_no")
    private Task task;

    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "work_center_id")
    private WorkCenter workCenter;

    @Column(name = "procedure_name")
    private String procedureName;

    @Column(name = "procedure_no")
    private Integer procedureNo;

    @Column(name = "procedure_type")
    private String procedureType;

    @Column(name = "machine_minutes")
    private int machineMinutes;

    @Column(name = "human_minutes")
    private int humanMinutes;

    private boolean rework;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "asp_procedure_no_next")
    @Column(name = "next_procedure_no")
    private List<Integer> nextProcedureNo;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonIgnore
    @JoinTable(name = "aps_procedure_next")
    private List<Procedure> nextProcedure;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "plan_start_date")
    private LocalDate planStartDate;

    @Column(name = "plan_end_date")
    private LocalDate planEndDate;

    private String status;

    private boolean parallel;

    @Column(name = "p_index")
    private int index;

    @Column(name = "p_level")
    private Integer level;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    public void addNextProcedure(Procedure procedure) {
        if (this.nextProcedure == null) {
            this.nextProcedure = new ArrayList<>();
        }
        this.nextProcedure.add(procedure);
    }

}
