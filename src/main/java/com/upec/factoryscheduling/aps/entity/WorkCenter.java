package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import org.optaplanner.core.api.domain.lookup.PlanningId;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "work_center")
@Data
public class WorkCenter {

    @Id
    @PlanningId
    private String id;
    private String workCenterCode;
    private String name;
    private String status;

}
