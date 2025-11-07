package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.service.ProcedureService;
import org.springframework.beans.factory.annotation.Autowired;
import com.upec.factoryscheduling.utils.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/procedure")
@CrossOrigin
public class ProcedureController {

    private ProcedureService procedureService;


    @Autowired
    public void setProcedureService(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }
    


    @PostMapping
    public ApiResponse<List<Timeslot>> createProcesses(@RequestBody List<Procedure> procedures) {
        return ApiResponse.success(new ArrayList<>());
    }


    @PostMapping("/list")
    public ApiResponse<List<Timeslot>> createProcedure(@RequestBody List<Procedure> procedures) {
        return ApiResponse.success(new ArrayList<>());
    }
}
