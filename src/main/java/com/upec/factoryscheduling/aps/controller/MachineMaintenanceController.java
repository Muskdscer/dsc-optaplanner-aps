package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import com.upec.factoryscheduling.aps.service.WorkCenterMaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import com.upec.factoryscheduling.utils.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作中心维护控制器
 * <p>提供设备维护计划相关的REST API端点，支持自动生成维护计划、批量创建和批量更新维护记录等操作。</p>
 * <p>该控制器确保工厂设备维护计划的高效管理，为生产调度系统提供准确的设备可用性信息。</p>
 */
@RestController  // 标记此类为REST控制器
@RequestMapping("/api/maintenance")  // 设置API基础路径
@CrossOrigin  // 允许跨域请求
public class MachineMaintenanceController {

    /** 工作中心维护服务 - 提供设备维护相关的业务逻辑 */
    private WorkCenterMaintenanceService maintenanceService;

    /**
     * 设置维护服务
     * @param maintenanceService 工作中心维护服务实例，通过依赖注入自动装配
     */
    @Autowired
    public void setMaintenanceService(WorkCenterMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    /**
     * 自动生成所有设备的维护计划
     * <p>为系统中的所有工作中心（设备）自动生成未来30天的维护计划。</p>
     * 
     * @return 包含生成的维护计划列表的响应实体，HTTP状态码为200 OK
     */
    @PostMapping("/auto")  // HTTP POST请求，路径为/api/maintenance/auto
    public ApiResponse<List<WorkCenterMaintenance>> auto() {
        return ApiResponse.success(maintenanceService.auto());
    }

    /**
     * 批量创建或更新维护计划
     * <p>批量保存或更新维护计划列表，根据日期检查记录是否已存在。</p>
     * 
     * @param maintenances 请求体中的维护计划对象列表
     * @return 无内容的响应实体，HTTP状态码为200 OK
     */
    @PostMapping("/all")  // HTTP POST请求，路径为/api/maintenance/all
    public ApiResponse<Void> createAll(@RequestBody List<WorkCenterMaintenance> maintenances) {
        maintenanceService.saveAllMaintenance(maintenances);
        return ApiResponse.success();  // 返回成功响应
    }

    /**
     * 批量更新维护计划
     * <p>批量更新维护计划列表中的记录，包括状态、时间和容量等信息。</p>
     * 
     * @param maintenances 请求体中的维护计划对象列表，包含更新信息
     * @return 包含更新后的维护计划列表的响应实体，HTTP状态码为200 OK
     */
    @PostMapping("/updateAll")  // HTTP POST请求，路径为/api/maintenance/updateAll
    public ApiResponse<List<WorkCenterMaintenance>> updateAll(@RequestBody List<WorkCenterMaintenance> maintenances){
        return ApiResponse.success(maintenanceService.updateAll(maintenances));
    }
}
