package com.upec.factoryscheduling.mes.service;


import com.upec.factoryscheduling.common.utils.JdbcTemplatePaginationUtils;
import com.upec.factoryscheduling.mes.entity.MesJjOrderTask;
import com.upec.factoryscheduling.mes.repository.MesJjOrderTaskRepository;
import com.upec.factoryscheduling.mes.response.OrderTaskQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesJjOrderTaskService {

    private MesJjOrderTaskRepository mesJjOrderTaskRepository;

    @Autowired
    @Qualifier("mySqlTemplate")
    private JdbcTemplate oracleTemplate;

    @Autowired
    private void setMesJjOrderTaskRepository(MesJjOrderTaskRepository mesJjOrderTaskRepository) {
        this.mesJjOrderTaskRepository = mesJjOrderTaskRepository;
    }

    public List<MesJjOrderTask> queryAllByOrderNoInAndTaskStatusIn(List<String> orderNos, List<String> taskStatus) {
        return mesJjOrderTaskRepository.queryAllByOrderNoInAndTaskStatusIn(orderNos, taskStatus);
    }

    /**
     * 使用MySQL Template实现MES_JJ_ORDER和MES_JJ_ORDER_TASK关联查询，支持分页
     * 所有查询条件直接拼接到SQL语句中
     *
     * @param orderName  订单名称
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param statusList 状态列表
     * @param pageNum    页码，从1开始
     * @param pageSize   每页数量
     * @return 返回分页结果Page<OrderTaskQueryResponse>
     */
    public Page<OrderTaskQueryResponse> findOrderTasksByConditionsWithPagination(String orderName, String startTime, String endTime,
                                                                                 List<String> statusList, Integer pageNum, Integer pageSize) {
        // 构建查询SQL（包含排序）
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT T.*, O.PLAN_QUANTITY AS ORDERPLANQUANTITY, O.ORDER_STATUS AS ORDERSTATUS, O.CONTRACTNUM, ")
                .append("t2.PRODUCT_CODE ,t2.PRODUCT_NAME ")
                .append("FROM MES_JJ_ORDER_TASK t ")
                .append("LEFT JOIN MES_JJ_ORDER o ON t.ORDERNO = o.ORDERNO ")
                .append("LEFT JOIN MES_JJ_ORDER_PRODUCT_INFO T2 ON T2.ORDERNO=T.ORDERNO ")
                .append("WHERE 1=1 ");

        // 添加查询条件
        if (orderName != null && !orderName.isEmpty()) {
            String likePattern = "%" + orderName + "%";
            sqlBuilder.append("AND (t.ORDERNO LIKE '").append(likePattern).append("' OR o.CONTRACTNUM LIKE '").append(likePattern).append("') ");
        }

        // 时间范围查询条件
        if (startTime != null && !startTime.isEmpty()) {
            sqlBuilder.append("AND t.PLAN_STARTDATE >= '").append(startTime).append("' ");
        }
        if (endTime != null && !endTime.isEmpty()) {
            sqlBuilder.append("AND t.PLAN_STARTDATE <= '").append(endTime).append("' ");
        }

        // 任务状态查询条件
        if (statusList != null && !statusList.isEmpty()) {
            StringBuilder inClause = new StringBuilder("AND t.TASK_STATUS IN ('");
            for (int i = 0; i < statusList.size(); i++) {
                if (i > 0) {
                    inClause.append("', '");
                }
                inClause.append(statusList.get(i));
            }
            inClause.append("') ");
            sqlBuilder.append(inClause);
        }
        // 添加排序
        sqlBuilder.append("ORDER BY  T.ORDERNO DESC, TO_NUMBER(SUBSTR(T.TASKNO,INSTR(T.TASKNO,'_')+1,LENGTH(T.TASKNO))), " +
                "T.PLAN_STARTDATE DESC");
        // 使用简化的分页工具类执行查询
        return JdbcTemplatePaginationUtils.queryForPage(
                oracleTemplate,
                sqlBuilder.toString(),
                new BeanPropertyRowMapper<>(OrderTaskQueryResponse.class),
                pageNum,
                pageSize
        );
    }
}
