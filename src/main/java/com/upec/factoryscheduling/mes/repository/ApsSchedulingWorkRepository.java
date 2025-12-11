package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.ApsSchedulingWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApsSchedulingWorkRepository extends JpaRepository<ApsSchedulingWork, String> {
}
