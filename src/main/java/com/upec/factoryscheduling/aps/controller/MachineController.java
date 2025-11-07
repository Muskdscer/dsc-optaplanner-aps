package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.service.WorkCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import com.upec.factoryscheduling.utils.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作中心（设备）控制器
 * <p>提供工作中心（设备/机器）相关的REST API端点，支持工作中心的查询、创建、更新和删除操作。</p>
 * <p>该控制器处理工厂设备的基础管理功能，为工厂调度系统提供设备资源数据。</p>
 */
@RestController  // 标记此类为REST控制器
@RequestMapping("/api/machines")  // 设置API基础路径
@CrossOrigin  // 允许跨域请求
public class MachineController {

    /** 工作中心服务 - 提供工作中心相关的业务逻辑 */
    private final WorkCenterService workCenterService;

    /**
     * 构造函数
     * @param workCenterService 工作中心服务实例，通过依赖注入自动装配
     */
    @Autowired
    public MachineController(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }

    /**
     * 获取所有工作中心
     * <p>查询系统中所有可用的工作中心（设备/机器）列表。</p>
     * 
     * @return 包含工作中心列表的响应实体，HTTP状态码为200 OK
     */
    @GetMapping  // HTTP GET请求，路径为/api/machines
    public ApiResponse<List<WorkCenter>> getAllMachines() {
        return ApiResponse.success(workCenterService.getAllMachines());
    }

    /**
     * 根据ID获取工作中心
     * <p>根据指定的ID查询特定的工作中心（设备/机器）信息。</p>
     * 
     * @param id 工作中心ID路径参数
     * @return 如果找到工作中心，返回包含工作中心对象的响应实体（状态码200 OK）；否则返回404 Not Found
     */
    @GetMapping("/{id}")  // HTTP GET请求，路径为/api/machines/{id}
    public ApiResponse<WorkCenter> getMachineById(@PathVariable String id) {  // 从URL路径中提取ID参数
        // 调用服务层方法获取工作中心，返回Optional对象
        return workCenterService.getMachineById(id)
                .map(ApiResponse::success)  // 找到记录时返回成功响应
                .orElse(ApiResponse.error("未找到指定ID的工作中心"));  // 未找到记录时返回错误响应
    }

    /**
     * 创建工作中心
     * <p>批量创建新的工作中心（设备/机器）记录。</p>
     * 
     * @param machines 请求体中的工作中心对象列表
     * @return 包含创建后的工作中心列表的响应实体，HTTP状态码为200 OK
     */
    @PostMapping  // HTTP POST请求，路径为/api/machines
    public ApiResponse<List<WorkCenter>> createMachines(@RequestBody List<WorkCenter> machines) {  // 从请求体中提取工作中心列表
        return ApiResponse.success(workCenterService.create(machines));
    }

    /**
     * 更新工作中心
     * <p>根据指定的ID更新工作中心（设备/机器）的信息。</p>
     * 
     * @param id 工作中心ID路径参数
     * @param machine 请求体中的工作中心对象，包含更新信息
     * @return 包含更新后的工作中心对象的响应实体，HTTP状态码为200 OK
     */
    @PutMapping("/{id}")  // HTTP PUT请求，路径为/api/machines/{id}
    public ApiResponse<WorkCenter> updateMachine(@PathVariable String id, @RequestBody WorkCenter machine) {
        return ApiResponse.success(workCenterService.updateMachine(id, machine));
    }

    /**
     * 删除工作中心
     * <p>根据指定的ID删除工作中心（设备/机器）记录。</p>
     * 
     * @param id 工作中心ID路径参数
     * @return 无内容的响应实体，HTTP状态码为200 OK
     */
    @DeleteMapping("/{id}")  // HTTP DELETE请求，路径为/api/machines/{id}
    public ApiResponse<Void> deleteMachine(@PathVariable String id) {
        workCenterService.deleteMachine(id);
        return ApiResponse.success();  // 返回成功响应，无响应体
    }
}
