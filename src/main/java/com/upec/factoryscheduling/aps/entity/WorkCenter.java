package com.upec.factoryscheduling.aps.entity;

import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.lookup.PlanningId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "aps_work_center")
@Getter
@Setter
public class WorkCenter {

    @Id
    @PlanningId
    private String id;
    @Column(name = "work_center_code")
    private String workCenterCode;
    private String name;
    private String status;

}
