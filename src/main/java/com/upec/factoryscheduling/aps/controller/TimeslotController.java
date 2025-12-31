package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.service.TimeslotService;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/create")
    public ApiResponse<Void> createTimeslot(
            @RequestParam("taskNos") List<String> taskNos,
            @RequestParam("timeslotIds") List<String> timeslotIds,
            @RequestParam(value = "time", defaultValue = "0.5") double time,
            @RequestParam(value = "slice", defaultValue = "0") int slice) {
        timeslotService.createTimeslot(taskNos, timeslotIds, time, slice);
        return ApiResponse.success();
    }


    @GetMapping("/{taskNo}/list")
    public ApiResponse<List<Timeslot>> findAllTimeslotByTaskNoIn(@PathVariable("taskNo") String taskNo) {
        return ApiResponse.success(timeslotService.findAllByTaskIn(List.of(taskNo)));
    }


    @PostMapping("/{timeslotId}/split")
    public ApiResponse<Void> splitOutsourcingTimeslot(@PathVariable("timeslotId") String timeslotId,
                                                      @RequestParam("days") int days) {
        timeslotService.splitOutsourcingTimeslot(timeslotId, days);
        return ApiResponse.success();
    }

}
