package com.upec.factoryscheduling.mes.controller;

import com.upec.factoryscheduling.mes.service.MesOrderService;
import com.upec.factoryscheduling.mes.service.MesJjOrderTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mesOrders")
@CrossOrigin
public class MesOrderController {

    @Autowired
    private MesOrderService mesOrderService;
    
    @Autowired
    private MesJjOrderTaskService mesJjOrderTaskService;

    @PostMapping("syncData")
    public ResponseEntity<Void> syncData(@RequestBody List<String> orderNos) {
        mesOrderService.mergePlannerData(orderNos);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 根据条件查询订单任务数据（旧接口，不分页）
     */
    @GetMapping("orderTasks")
    public ResponseEntity<List<?>> queryOrderTasks(
            @RequestParam(required = false) String orderName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) List<String> statusList) {
        
        List<?> result = mesJjOrderTaskService.findOrderTasksByConditions(orderName, startTime, endTime, statusList);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 根据条件分页查询订单任务数据（新接口，支持关联查询和分页）
     */
    @GetMapping("orderTasks/page")
    public ResponseEntity<Map<String, Object>> queryOrderTasksWithPagination(
            @RequestParam(required = false) String orderName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) List<String> statusList,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        Map<String, Object> result = mesJjOrderTaskService.findOrderTasksByConditionsWithPagination(
                orderName, startTime, endTime, statusList, pageNum, pageSize);
        return ResponseEntity.ok(result);
    }
}
