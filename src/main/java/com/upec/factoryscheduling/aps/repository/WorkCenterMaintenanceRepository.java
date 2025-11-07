package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkCenterMaintenanceRepository extends JpaRepository<WorkCenterMaintenance, String> {

    WorkCenterMaintenance findFirstByWorkCenterAndDate(WorkCenter machine, LocalDate date);

    WorkCenterMaintenance findFirstByDateAndWorkCenterIsNull(LocalDate date);

    List<WorkCenterMaintenance> findAllByWorkCenterInAndDateBetween(List<WorkCenter> machines, LocalDate start, LocalDate end);
}
