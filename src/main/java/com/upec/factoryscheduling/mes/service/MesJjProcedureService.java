package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.entity.MesJjProcedure;
import com.upec.factoryscheduling.mes.repository.MesJjProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesJjProcedureService {

    private MesJjProcedureRepository mesJjProcedureRepository;

    @Autowired
    public void setMesJjProcedureRepository(MesJjProcedureRepository mesJjProcedureRepository) {
        this.mesJjProcedureRepository = mesJjProcedureRepository;
    }
    public List<MesJjProcedure> findAllByTaskNo(List<String> taskNos) {
        return mesJjProcedureRepository.findAllByTaskNoIn(taskNos);
    }
}
