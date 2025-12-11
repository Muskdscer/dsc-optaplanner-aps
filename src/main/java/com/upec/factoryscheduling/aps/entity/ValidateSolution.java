package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Setter
@Getter
public class ValidateSolution {


    private Long id;

    private Procedure procedure;

    private Order order;

    private WorkCenter machine;

    private WorkCenterMaintenance maintenance;

    private int dailyHours;

    private LocalDateTime dateTime;

    private boolean isManual;

    private String message;

    public ValidateSolution() {
    }

    public ValidateSolution(Procedure procedure, Order order, WorkCenter machine, WorkCenterMaintenance maintenance) {
        this.procedure = procedure;
        this.order = order;
        this.machine = machine;
        this.maintenance = maintenance;
    }


    public void setMessage(String message) {
        if (this.message != null) {
            this.message = this.message + ";" + message;
        } else {
            this.message = message;
        }
    }
}
