package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import org.optaplanner.core.api.domain.entity.PlanningEntity;

import java.util.List;

@Data
@PlanningEntity
public class MaintenanceGroup {
    private String id;
    private List<WorkCenterMaintenance> maintenances;
}
