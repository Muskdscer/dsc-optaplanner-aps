package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesBaseWorkCenterRepository extends JpaRepository<MesBaseWorkCenter, String> {

    List<MesBaseWorkCenter> findAllByWorkCenterCodeIn(List<String> workCenterCodes);
}
