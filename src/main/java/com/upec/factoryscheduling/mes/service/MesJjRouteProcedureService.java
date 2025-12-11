package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.entity.MesJjRouteProcedure;
import com.upec.factoryscheduling.mes.repository.MesJjRouteProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesJjRouteProcedureService {

    private MesJjRouteProcedureRepository mesJjRouteProcedureRepository;

    @Autowired
    public void setMesJjRouteProcedureRepository(MesJjRouteProcedureRepository mesJjRouteProcedureRepository) {
        this.mesJjRouteProcedureRepository = mesJjRouteProcedureRepository;
    }

    public List<MesJjRouteProcedure> findAllByRouteSeqIn(List<String> routeSeq) {
        return mesJjRouteProcedureRepository.findAllByRouteSeqIn(routeSeq);
    }
}
