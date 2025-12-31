package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.service.WorkCenterService;
import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import com.upec.factoryscheduling.mes.repository.MesBaseWorkCenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MesBaseWorkCenterService {


    private MesBaseWorkCenterRepository mesBaseWorkCenterRepository;

    @Autowired
    public void setMesBaseWorkCenterRepository(MesBaseWorkCenterRepository mesBaseWorkCenterRepository) {
        this.mesBaseWorkCenterRepository = mesBaseWorkCenterRepository;
    }

    private WorkCenterService workCenterService;
    @Autowired
    public void setWorkCenterService(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }


    public List<MesBaseWorkCenter> findAllByWorkCenterCodeIn(List<String> workCenterCodes) {
        return mesBaseWorkCenterRepository.findAllByWorkCenterCodeIn(workCenterCodes);
    }

    public List<MesBaseWorkCenter> findByIdIn(List<String> workCenterId) {
        return mesBaseWorkCenterRepository.findAllById(workCenterId);
    }

    /**
     * 获取所有工作中心
     *
     * @return 所有工作中心列表
     */
    public List<MesBaseWorkCenter> findAll() {
        return mesBaseWorkCenterRepository.findAll();
    }


    public List<MesBaseWorkCenter> findAllByFactorySeq(String factorySeq) {
        return mesBaseWorkCenterRepository.findAllByFactorySeq(factorySeq);
    }


    private List<WorkCenter> convertWorkCenters(List<MesBaseWorkCenter> mesBaseWorkCenters) {
        List<WorkCenter> workCenters = new ArrayList<>();
        for (MesBaseWorkCenter baseWorkCenter : mesBaseWorkCenters) {
            WorkCenter workCenter = new WorkCenter();
            workCenter.setId(baseWorkCenter.getSeq());
            workCenter.setName(baseWorkCenter.getDescription());
            workCenter.setWorkCenterCode(baseWorkCenter.getWorkCenterCode());
            workCenter.setStatus(baseWorkCenter.getStatus());
            workCenters.add(workCenter);
        }
        return workCenterService.saveWorkCenters(workCenters);
    }


    @Transactional("oracleTransactionManager")
    public void asyncWorkCenterData() {
        List<MesBaseWorkCenter> mesBaseWorkCenters = findAllByFactorySeq("2");
        convertWorkCenters(mesBaseWorkCenters);
    }
}
