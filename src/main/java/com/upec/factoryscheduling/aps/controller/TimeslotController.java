package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.service.TimeslotService;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timeslot")
@Slf4j
@CrossOrigin
public class TimeslotController {

    private TimeslotService timeslotService;

    @Autowired
    public void setTimeslotService(TimeslotService timeslotService) {
        this.timeslotService = timeslotService;
    }

    @GetMapping("/list")
    public ApiResponse<FactorySchedulingSolution> queryTimeslot() {
        return ApiResponse.success(timeslotService.findAll());
    }

}
