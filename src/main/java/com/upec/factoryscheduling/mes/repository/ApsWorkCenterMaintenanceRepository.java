package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.ApsWorkCenterMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApsWorkCenterMaintenanceRepository extends JpaRepository<ApsWorkCenterMaintenance, String> {

    List<ApsWorkCenterMaintenance> findAllByWorkCenterCodeIn(List<String> workCenterCodes);
    
    // 自定义查询获取所有工作中心ID
    @Query(value = "SELECT DISTINCT b.SEQ FROM MES_BASE_WORKCENTER b", nativeQuery = true)
    List<String> findAllWorkCenterIds();
}
