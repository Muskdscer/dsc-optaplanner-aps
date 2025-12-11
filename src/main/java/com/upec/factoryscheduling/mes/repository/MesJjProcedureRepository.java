package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesJjProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesJjProcedureRepository extends JpaRepository<MesJjProcedure, String> {

    List<MesJjProcedure> findAllByTaskNoIn(List<String> taskNos);

}
