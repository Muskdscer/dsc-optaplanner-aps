package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkCenterRepository extends JpaRepository<WorkCenter, String> {

}
