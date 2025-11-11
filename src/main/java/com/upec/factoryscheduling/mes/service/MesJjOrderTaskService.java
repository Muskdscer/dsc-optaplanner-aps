package com.upec.factoryscheduling.mes.service;


import com.upec.factoryscheduling.mes.dto.OrderTaskQueryDTO;
import com.upec.factoryscheduling.mes.entity.MesJjOrderTask;
import com.upec.factoryscheduling.mes.repository.MesJjOrderTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MesJjOrderTaskService {

    private MesJjOrderTaskRepository mesJjOrderTaskRepository;
    
    @Autowired
    @Qualifier("mySqlTemplate")
    private JdbcTemplate mySqlTemplate;

    @Autowired
    private void setMesJjOrderTaskRepository(MesJjOrderTaskRepository mesJjOrderTaskRepository) {
        this.mesJjOrderTaskRepository = mesJjOrderTaskRepository;
    }

    public List<MesJjOrderTask> queryAllByOrderNoInAndTaskStatus(List<String> orderNos, List<String> taskStatus) {
        return mesJjOrderTaskRepository.queryAllByOrderNoInAndTaskStatusIn(orderNos, taskStatus);
    }
    
    /**
     * 使用MySQL Template实现MES_JJ_ORDER和MES_JJ_ORDER_TASK关联查询，支持分页
     * @param orderName 订单名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param statusList 状态列表
     * @param pageNum 页码，从1开始
     * @param pageSize 每页数量
     * @return 包含数据列表和总数的Map
     */
    public Map<String, Object> findOrderTasksByConditionsWithPagination(String orderName, String startTime, String endTime, 
                                                                      List<String> statusList, Integer pageNum, Integer pageSize) {
        // 参数验证
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20; // 默认每页20条
        }
        
        // 构建查询条件
        StringBuilder sqlBuilder = new StringBuilder();
        StringBuilder countBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        // 基础SQL查询，关联订单表和任务表
        sqlBuilder.append("SELECT t.*, o.PLAN_QUANTITY as orderPlanQuantity, o.ORDER_STATUS as orderStatus, o.CONTRACTNUM ")
                .append("FROM MES_JJ_ORDER_TASK t ")
                .append("LEFT JOIN MES_JJ_ORDER o ON t.ORDERNO = o.ORDERNO ")
                .append("WHERE 1=1 ");
        
        countBuilder.append("SELECT COUNT(*) ")
                .append("FROM MES_JJ_ORDER_TASK t ")
                .append("LEFT JOIN MES_JJ_ORDER o ON t.ORDERNO = o.ORDERNO ")
                .append("WHERE 1=1 ");
        
        // 订单号模糊查询条件
        if (orderName != null && !orderName.isEmpty()) {
            sqlBuilder.append("AND (t.ORDERNO LIKE ? OR o.CONTRACTNUM LIKE ?) ");
            countBuilder.append("AND (t.ORDERNO LIKE ? OR o.CONTRACTNUM LIKE ?) ");
            String likePattern = "%" + orderName + "%";
            params.add(likePattern);
            params.add(likePattern);
        }
        
        // 时间范围查询条件
        if (startTime != null && !startTime.isEmpty()) {
            sqlBuilder.append("AND t.PLAN_STARTDATE >= ? ");
            countBuilder.append("AND t.PLAN_STARTDATE >= ? ");
            params.add(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            sqlBuilder.append("AND t.PLAN_ENDDATE <= ? ");
            countBuilder.append("AND t.PLAN_ENDDATE <= ? ");
            params.add(endTime);
        }
        
        // 任务状态查询条件
        if (statusList != null && !statusList.isEmpty()) {
            StringBuilder statusSql = new StringBuilder();
            for (int i = 0; i < statusList.size(); i++) {
                if (i > 0) {
                    statusSql.append(",? ");
                } else {
                    statusSql.append("? ");
                }
                params.add(statusList.get(i));
            }
            sqlBuilder.append("AND t.TASK_STATUS IN (").append(statusSql).append(") ");
            countBuilder.append("AND t.TASK_STATUS IN (").append(statusSql).append(") ");
        }
        
        // 添加排序
        sqlBuilder.append("ORDER BY t.PLAN_STARTDATE DESC ");
        
        // 添加分页
        sqlBuilder.append("LIMIT ? OFFSET ?");
        int offset = (pageNum - 1) * pageSize;
        params.add(pageSize);
        params.add(offset);


        // 执行总数查询
        Integer total = mySqlTemplate.queryForObject(countBuilder.toString(), Integer.class, params.subList(0, params.size() - 2).toArray());
        
        // 使用RowMapper直接将查询结果映射到DTO对象
        RowMapper<OrderTaskQueryDTO> rowMapper = new RowMapper<OrderTaskQueryDTO>() {
            @Override
            public OrderTaskQueryDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                OrderTaskQueryDTO dto = new OrderTaskQueryDTO();
                dto.setTaskNo(rs.getString("TASKNO"));
                dto.setOrderNo(rs.getString("ORDERNO"));
                dto.setContractNum(rs.getString("CONTRACTNUM"));
                dto.setOrderName(rs.getString("CONTRACTNUM")); // 使用合同编号作为订单名称
                dto.setRouteSeq(rs.getString("ROUTE_SEQ"));
                dto.setPlanQuantity(rs.getInt("PLAN_QUANTITY"));
                dto.setTaskStatus(rs.getString("TASK_STATUS"));
                dto.setPlanStartDate(rs.getDate("PLAN_STARTDATE"));
                dto.setPlanEndDate(rs.getDate("PLAN_ENDDATE"));
                dto.setFactStartDate(rs.getDate("FACT_STARTDATE"));
                dto.setFactEndDate(rs.getDate("FACT_ENDDATE"));
                dto.setOrderPlanQuantity(rs.getInt("orderPlanQuantity"));
                dto.setOrderStatus(rs.getString("orderStatus"));
                return dto;
            }
        };
        
        // 执行分页查询
        List<OrderTaskQueryDTO> dtoList = mySqlTemplate.query(sqlBuilder.toString(), params.toArray(), rowMapper);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", dtoList);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize); // 计算总页数
        
        return result;
    }
    
    /**
     * 根据订单名称、时间范围和状态查询订单任务
     */
    public List<?> findOrderTasksByConditions(String orderName, String startTime, String endTime, List<String> statusList) {
        return mesJjOrderTaskRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 按订单号模糊查询
            if (orderName != null && !orderName.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("orderNo"), "%" + orderName + "%"));
            }
            
            // 按计划开始时间范围查询
            if (startTime != null && !startTime.isEmpty()) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("planStartDate"), startTime));
            }
            if (endTime != null && !endTime.isEmpty()) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("planEndDate"), endTime));
            }
            
            // 按任务状态查询
            if (statusList != null && !statusList.isEmpty()) {
                predicates.add(root.get("taskStatus").in(statusList));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
