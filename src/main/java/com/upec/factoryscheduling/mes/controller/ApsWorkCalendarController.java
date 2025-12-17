package com.upec.factoryscheduling.mes.controller;

import com.upec.factoryscheduling.common.utils.ApiResponse;
import com.upec.factoryscheduling.mes.entity.ApsWorkCenterMaintenance;
import com.upec.factoryscheduling.mes.request.WorkCalendarRequest;
import com.upec.factoryscheduling.mes.response.WorkCalendarResponse;
import com.upec.factoryscheduling.mes.service.ApsWorkCenterMaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作日历管理控制器
 * 提供创建、查询工作日历的REST API接口
 */
@RestController
@RequestMapping("/api/work-calendar")
@Validated
public class ApsWorkCalendarController {

    private ApsWorkCenterMaintenanceService workCenterMaintenanceService;

    @Autowired
    public void setWorkCenterMaintenanceService(ApsWorkCenterMaintenanceService workCenterMaintenanceService) {
        this.workCenterMaintenanceService = workCenterMaintenanceService;
    }

    /**
     * 为所有工作中心批量创建工作日历
     * 此接口根据指定的日期范围，为系统中的所有工作中心统一创建工作日历数据
     * 每个工作日历将设置为默认工作时间：9:00-17:30，容量为480分钟
     *
     * @param request 包含开始日期和结束日期的请求对象
     * @return 创建结果信息，包含创建的记录总数
     */
    @PostMapping("/create-all")
    public ApiResponse<WorkCalendarResponse> createWorkCalendarForAllCenters(
            @RequestBody WorkCalendarRequest request) {

        try {
            // 验证日期范围的有效性
            if (!request.isValidDateRange()) {
                return ApiResponse.error("开始日期不能晚于结束日期");
            }

            // 执行创建操作
            int totalCreated = workCenterMaintenanceService.createWorkCalendarForAllCenters(
                    request.getStartDate(), request.getEndDate());

            // 检查是否创建了任何记录
            if (totalCreated == 0) {
                return ApiResponse.error("未找到任何工作中心数据，未创建工作日历");
            }

            // 构建成功响应结果
            WorkCalendarResponse successData = WorkCalendarResponse.success(
                    totalCreated, request.getStartDate(), request.getEndDate());

            return ApiResponse.success(successData, "工作日历创建成功");
        } catch (Exception e) {
            // 处理异常情况
            return ApiResponse.error("创建工作日历失败：" + e.getMessage());
        }
    }


    @PostMapping("/update")
    public ApiResponse<Void> updateWorkCalendarForAllCenters(@RequestBody ApsWorkCenterMaintenance workCenterMaintenance) {
        workCenterMaintenanceService.update(workCenterMaintenance);
        return ApiResponse.successVoid("工作日历修改成功");
    }
}
