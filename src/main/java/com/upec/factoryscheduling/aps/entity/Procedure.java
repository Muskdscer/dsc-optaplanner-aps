package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.aps.solution.ProcedureVariableListener;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    //    @ShadowVariable(variableListenerClass = ProcedureVariableListener.class, sourceVariableName = "timeslots")
    private LocalDate planStartDate;

    //    @ShadowVariable(variableListenerClass = ProcedureVariableListener.class, sourceVariableName = "timeslots")
    private LocalDate planEndDate;

    private String status;

    private boolean parallel;

    private int index = 1;

    public void addNextProcedure(Procedure procedure) {
        if (this.nextProcedure == null) {
            this.nextProcedure = new ArrayList<>();
        }
        this.nextProcedure.add(procedure);
        if (this.index < this.index + 1) {
            procedure.setIndex(this.index + 1);
        }
    }

}
