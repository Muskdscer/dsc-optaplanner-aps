package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesJjRouteProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesJjRouteProcedureRepository extends JpaRepository<MesJjRouteProcedure, String> {
    List<MesJjRouteProcedure> findAllByRouteSeqIn(List<String> routeSeq);
}
