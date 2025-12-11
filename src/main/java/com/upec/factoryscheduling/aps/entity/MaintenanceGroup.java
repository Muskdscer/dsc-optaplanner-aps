package com.upec.factoryscheduling.aps.entity;

import lombok.Data;

import java.util.List;

@Data
public class MaintenanceGroup {
    private String workCenterCode;
    private List<WorkCenterMaintenance> maintenances;
}
