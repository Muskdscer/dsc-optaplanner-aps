package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.aps.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private OrderRepository orderRepository;

    @Autowired
    public void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    @Transactional("mysqlTransactionManager")
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional("mysqlTransactionManager")
    public List<Order> createOrders(List<Order> orders) {
        return orderRepository.saveAll(orders);
    }

    @Transactional("mysqlTransactionManager")
    public void deleteOrder(String id) {
        orderRepository.deleteById(id);
    }

    @Transactional("mysqlTransactionManager")
    public void deleteAll() {
        orderRepository.deleteAll();
    }

    @Transactional("oracleTransactionManager")
    public List<Order> saveAll(List<Order> orders) {
        return orderRepository.saveAll(orders);
    }
}
