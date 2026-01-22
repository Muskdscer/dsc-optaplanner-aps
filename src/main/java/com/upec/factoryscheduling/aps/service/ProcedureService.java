package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.aps.entity.TaskExt;
import com.upec.factoryscheduling.aps.repository.ProcedureRepository;
import com.upec.factoryscheduling.aps.resquest.ProcedureRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ProcedureService {

    private ProcedureRepository procedureRepository;

    private OrderTaskService orderTaskService;

    @Autowired
    public void setOrderTaskService(OrderTaskService orderTaskService) {
        this.orderTaskService = orderTaskService;
    }

    @Autowired
    public void setProcedureRepository(ProcedureRepository procedureRepository) {
        this.procedureRepository = procedureRepository;
    }

    @Transactional("mysqlTransactionManager")
    public List<Procedure> saveProcedures(List<Procedure> procedures) {
        return procedureRepository.saveAll(procedures);
    }

    @Transactional("mysqlTransactionManager")
    public Procedure saveProcedure(Procedure procedure) {
        return procedureRepository.save(procedure);
    }

    @Transactional("mysqlTransactionManager")
    public void deleteAll() {
        procedureRepository.deleteAll();
    }

    public List<Procedure> findAllByTaskNoIsIn(List<String> taskNos) {
        return procedureRepository.findAllByTask_TaskNoIsIn(taskNos);
    }

    /**
     * 分页查询工序列表
     *
     * @param request 查询参数
     * @return 分页结果
     */
    public Page<TaskExt> findProceduresByPage(ProcedureRequest request) {
        Page<Task> taskPage = orderTaskService.queryTask(
                request.getTaskNo(),
                null, null,
                request.getStatus(),
                request.getPageNum(),
                request.getPageSize());
        List<TaskExt> taskExts = new ArrayList<>();
        for (Task task : taskPage.getContent()) {
            TaskExt taskExt = new TaskExt();
            BeanUtils.copyProperties(task, taskExt);
            List<Procedure> procedures = procedureRepository.findAllByTask_TaskNo(task.getTaskNo());
            taskExt.setProcedures(procedures);
            taskExts.add(taskExt);
        }
        return new PageImpl<>(taskExts, taskPage.getPageable(), taskPage.getTotalElements());
    }

}
