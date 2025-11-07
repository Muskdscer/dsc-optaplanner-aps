package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.aps.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import com.upec.factoryscheduling.utils.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 * <p>提供订单相关的REST API端点，支持订单的查询、创建和删除操作。</p>
 * <p>该控制器处理工厂生产订单的基础管理功能，为工厂调度系统提供订单数据。</p>
 */
@RestController  // 标记此类为REST控制器
@RequestMapping("/api/orders")  // 设置API基础路径
@CrossOrigin  // 允许跨域请求
public class OrderController {

    /** 订单服务 - 提供订单相关的业务逻辑 */
    private  OrderService orderService;

    /**
     * 设置订单服务
     * @param orderService 订单服务实例，通过依赖注入自动装配
     */
    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 获取所有订单
     * <p>查询系统中所有的订单列表。</p>
     * 
     * @return 订单列表，包含所有订单对象
     */
    @GetMapping  // HTTP GET请求，路径为/api/orders
    public ApiResponse<List<Order>> getAllOrders() {
        return ApiResponse.success(orderService.getAllOrders());
    }

    /**
     * 根据ID获取订单
     * <p>根据指定的ID查询特定的订单信息。</p>
     * 
     * @param id 订单ID路径参数
     * @return 如果找到订单，返回包含订单对象的响应实体（状态码200 OK）；否则返回404 Not Found
     */
    @GetMapping("/{id}")  // HTTP GET请求，路径为/api/orders/{id}
    public ApiResponse<Order> getOrderById(@PathVariable String id) {  // 从URL路径中提取ID参数
        return orderService.getOrderById(id)
                .map(ApiResponse::success)  // 找到记录时返回成功响应
                .orElse(ApiResponse.error("未找到指定ID的订单"));  // 未找到记录时返回错误响应
    }

    /**
     * 创建订单
     * <p>批量创建新的订单记录。</p>
     * 
     * @param orders 请求体中的订单对象列表
     * @return 包含创建后的订单列表的响应实体，HTTP状态码为200 OK
     */
    @PostMapping  // HTTP POST请求，路径为/api/orders
    public ApiResponse<List<Order>> createOrders(@RequestBody List<Order> orders) {  // 从请求体中提取订单列表
        return ApiResponse.success(orderService.createOrders(orders));
    }

    /**
     * 删除订单
     * <p>根据指定的ID删除订单记录。</p>
     * 
     * @param id 订单ID路径参数
     * @return 无内容的响应实体，HTTP状态码为200 OK
     */
    @DeleteMapping("/{id}")  // HTTP DELETE请求，路径为/api/orders/{id}
    public ApiResponse<Void> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ApiResponse.success();  // 返回成功响应
    }
}
