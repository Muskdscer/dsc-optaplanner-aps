package com.upec.factoryscheduling.common.jobs;

import com.upec.factoryscheduling.aps.service.WorkCenterService;
import com.upec.factoryscheduling.mes.service.MesBaseWorkCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SchedulingJobService {

    @Autowired
    private MesBaseWorkCenterService mesBaseWorkCenterService;

    @Autowired
    private WorkCenterService workCenterService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional("mysqlTransactionManager")
    public void syncWorkCenterData() {
        workCenterService.deleteAll();
        mesBaseWorkCenterService.asyncWorkCenterData();
    }
}
