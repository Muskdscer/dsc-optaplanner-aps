package com.upec.factoryscheduling.mes.controller;

import com.upec.factoryscheduling.common.utils.ApiResponse;
import com.upec.factoryscheduling.mes.entity.ApsWorkCenterMaintenance;
import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import com.upec.factoryscheduling.mes.service.ApsWorkCenterMaintenanceService;
import com.upec.factoryscheduling.mes.service.MesBaseWorkCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mes_work_center")
@CrossOrigin
public class MesWorkCenterController {

    private MesBaseWorkCenterService mesBaseWorkCenterService;

    @Autowired
    public void setMesBaseWorkCenterService(MesBaseWorkCenterService mesBaseWorkCenterService) {
        this.mesBaseWorkCenterService = mesBaseWorkCenterService;
    }

    private ApsWorkCenterMaintenanceService apsWorkCenterMaintenanceService;

    @Autowired
    public void setApsWorkCenterMaintenanceService(ApsWorkCenterMaintenanceService apsWorkCenterMaintenanceService) {
        this.apsWorkCenterMaintenanceService = apsWorkCenterMaintenanceService;
    }


    @GetMapping("/list")
    public ApiResponse<List<MesBaseWorkCenter>> queryAllWorkCenter() {
        List<MesBaseWorkCenter> mesBaseWorkCenters = mesBaseWorkCenterService.findAllByFactorySeq("2");
        return ApiResponse.success(mesBaseWorkCenters);
    }

    @GetMapping("/maintenance/list")
    public ApiResponse<List<ApsWorkCenterMaintenance>> findAllByWorkCenterCodeAndDate(@RequestParam("workCenterCode") String workCenterCode,
                                                                                      @RequestParam("startDate") String startDate,
                                                                                      @RequestParam("endDate") String endDate) {
        return ApiResponse.success(apsWorkCenterMaintenanceService.findAllByWorkCenterCodeAndLocalDateBetween(workCenterCode, startDate, endDate));
    }
}
