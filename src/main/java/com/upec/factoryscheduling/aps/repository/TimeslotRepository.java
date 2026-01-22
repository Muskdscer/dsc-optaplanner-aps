package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, String> {

    List<Timeslot> findAllByProcedure_Task_TaskNoIsIn(List<String> taskNos);

    @Query("SELECT DISTINCT t FROM Timeslot t " +
           "JOIN FETCH t.procedure p " +
           "LEFT JOIN FETCH p.nextProcedure " +
           "WHERE p.task.taskNo IN :taskNos")
    List<Timeslot> findAllByProcedure_Task_TaskNoIsInWithNextProcedures(@Param("taskNos") List<String> taskNos);

    List<Timeslot> findAllByProcedure_Task_TaskNoIsIn(List<String> taskNos, Sort sort);

    List<Timeslot> findAllByProcedure(Procedure procedure);

    List<Timeslot> findAllByIdIsIn(Collection<String> ids);

    List<Timeslot> findAllByProcedureAndIdNot(Procedure procedure, String id);

}
