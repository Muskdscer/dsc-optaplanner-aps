package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.entity.MesJjProcedure;
import com.upec.factoryscheduling.mes.repository.MesJjProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class MesJjProcedureService {

    private MesJjProcedureRepository mesJjProcedureRepository;

    @Autowired
    public void setMesJjProcedureRepository(MesJjProcedureRepository mesJjProcedureRepository) {
        this.mesJjProcedureRepository = mesJjProcedureRepository;
    }


    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("oracleTemplate")
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MesJjProcedure> findAllByTaskNo(List<String> taskNos) {
        return mesJjProcedureRepository.findAllByTaskNoIn(taskNos);
    }

    public void syncProcedures() {
        String sql = " SELECT T1.* FROM MES_JJ_PROCEDURE T1 " +
                " INNER JOIN MES_JJ_ORDER T2 ON T2.ORDER_STATUS <> '生产完成' AND T1.ORDERNO = T2.ORDERNO " +
                " WHERE T1.CREATEDATE>='2025-01-01' AND T1.CREATEDATE<= '2025-01-31' " +
                " AND T1.SEQ NOT IN ( SELECT ID FROM APS_PROCEDURE ) ";
        List<MesJjProcedure> mesJjProcedures = jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(MesJjProcedure.class));
    }
}
