package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, String> {

    List<Timeslot> findAllByOrderIn(List<Order> orders, Sort sort);

    List<Timeslot> findAllByProcedureIdIn(Collection<String> procedureIds);

    List<Timeslot> findAllByTask_TaskNoIn(List<String> taskNos);
}
