package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, String> , JpaSpecificationExecutor<Procedure> {

    List<Procedure> findAllByTask_TaskNoIsIn(List<String> taskNos);

    List<Procedure> findAllByTask_TaskNo(String taskNo);
}
