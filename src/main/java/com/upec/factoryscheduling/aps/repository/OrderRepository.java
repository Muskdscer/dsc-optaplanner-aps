package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

}
