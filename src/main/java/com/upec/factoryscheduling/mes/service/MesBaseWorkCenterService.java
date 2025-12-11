package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import com.upec.factoryscheduling.mes.repository.MesBaseWorkCenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesBaseWorkCenterService {


    private MesBaseWorkCenterRepository mesBaseWorkCenterRepository;

    @Autowired
    public void setMesBaseWorkCenterRepository(MesBaseWorkCenterRepository mesBaseWorkCenterRepository) {
        this.mesBaseWorkCenterRepository = mesBaseWorkCenterRepository;
    }


    public List<MesBaseWorkCenter> findAllByWorkCenterCodeIn(List<String> workCenterCodes) {
        return mesBaseWorkCenterRepository.findAllByWorkCenterCodeIn(workCenterCodes);
    }

    public List<MesBaseWorkCenter> findByIdIn(List<String> workCenterId) {
        return mesBaseWorkCenterRepository.findAllById(workCenterId);
    }
    
    /**
     * 获取所有工作中心
     * @return 所有工作中心列表
     */
    public List<MesBaseWorkCenter> findAll() {
        return mesBaseWorkCenterRepository.findAll();
    }
}
