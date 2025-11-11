package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesJjOrderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MesJjOrderTaskRepository extends JpaRepository<MesJjOrderTask, String>, JpaSpecificationExecutor<MesJjOrderTask> {

    List<MesJjOrderTask> queryAllByOrderNoInAndTaskStatusIn(Collection<String> orderNos, Collection<String> taskStatuses);

}
