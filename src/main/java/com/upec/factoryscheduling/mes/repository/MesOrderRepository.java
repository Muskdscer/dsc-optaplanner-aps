package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesJjOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesOrderRepository extends JpaRepository<MesJjOrder, String> {

    List<MesJjOrder> findAllByOrderStatusIn(List<String> orderStatus);

    List<MesJjOrder> findAllByOrderNo(String orderNo);

    List<MesJjOrder> findAllByPlanStartDateAfterAndOrderStatusIn(String planStartDate, List<String> orderStatus);

    List<MesJjOrder> findAllByOrderNoIn(List<String> orderNos);

}
