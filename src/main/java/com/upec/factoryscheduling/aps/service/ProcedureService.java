package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.repository.ProcedureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProcedureService {

    private ProcedureRepository procedureRepository;

    @Autowired
    public void setProcedureRepository(ProcedureRepository procedureRepository) {
        this.procedureRepository = procedureRepository;
    }

    @Transactional("h2TransactionManager")
    public List<Procedure> saveProcedures(List<Procedure> procedures) {
        return procedureRepository.saveAll(procedures);
    }

    @Transactional("h2TransactionManager")
    public Procedure saveProcedure(Procedure procedure) {
        return procedureRepository.save(procedure);
    }

    @Transactional("h2TransactionManager")
    public void deleteAll() {
        procedureRepository.deleteAll();
    }


    public List<Procedure> findAllByIdIsIn(List<String> ids) {
        return procedureRepository.findAllByIdIsIn(ids);
    }

    public List<Procedure> findAllByTaskNoIsIn(List<String> taskNos) {
        return procedureRepository.findAllByTaskNoIsIn(taskNos);
    }

}
