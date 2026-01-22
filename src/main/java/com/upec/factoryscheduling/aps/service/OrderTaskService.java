package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.aps.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderTaskService {

    private TaskRepository taskRepository;


    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional("oracleTransactionManager")
    public List<Task> saveAll(List<Task> tasks) {
        return taskRepository.saveAll(tasks);
    }

    /**
     * 使用JPA实现的分页查询方法，通过开始时间、结束时间、任务号模糊查询和任务状态过滤
     *
     * @param taskNo     任务号（模糊查询）
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param taskStatus 任务状态
     * @param pageNum    页码，从1开始
     * @param pageSize   每页数量
     * @return 分页结果
     */
    public Page<Task> queryTask(
            String taskNo, String startTime, String endTime, String taskStatus, Integer pageNum, Integer pageSize) {
        // 参数验证
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }

        if (pageSize == null || pageSize < 1) {
            pageSize = 20; // 默认每页20条
        }

        // 创建分页对象
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "planStartDate"));
        Specification<Task> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 任务号模糊查询
            if (taskNo != null && !taskNo.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("taskNo"), "%" + taskNo + "%"));
            }
            // 计划开始时间范围查询
            if (startTime != null && !startTime.isEmpty()) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("planStartDate"), LocalDate.parse(startTime)));
            }
            // 计划结束时间范围查询
            if (endTime != null && !endTime.isEmpty()) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("planEndDate"), LocalDate.parse(endTime)));
            }
            // 任务状态过滤
            if (taskStatus != null && !taskStatus.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), taskStatus));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        // 使用JPA Criteria API构建查询
        return taskRepository.findAll(specification, pageable);
    }

    public List<Task> findAllByTaskNoIsIn(List<String> taskNos) {
        return taskRepository.findAllByTaskNoIsIn(taskNos);
    }


    public Task findById(String taskNo) {
        return taskRepository.findById(taskNo).orElse(null);
    }

    @Transactional("mysqlTransactionManager")
    public void save(Task task) {
        taskRepository.save(task);
    }
}
