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

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    @Transactional("h2TransactionManager")
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional("h2TransactionManager")
    public List<Order> createOrders(List<Order> orders) {
        return orderRepository.saveAll(orders);
    }

    @Transactional("h2TransactionManager")
    public void deleteOrder(String id) {
        orderRepository.deleteById(id);
    }

    @Transactional("h2TransactionManager")
    public void deleteAll() {
        orderRepository.deleteAll();
    }

    public List<Order> queryByOrderNoIn(List<String> orderNos) {
        return orderRepository.queryByOrderNoIn(orderNos);
    }

    @Transactional("h2TransactionManager")
    public List<Order> saveAll(List<Order> orders) {
        return orderRepository.saveAll(orders);
    }
}
