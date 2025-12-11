package com.upec.factoryscheduling.mes.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class OrderTaskQueryResponse implements Serializable {
    // 任务相关字段
    private String taskNo;        // 任务编号
    private String orderNo;       // 订单编号
    private String orderName;     // 订单名称
    private String routeSeq;      // 工艺路线
    private Integer planQuantity; // 计划数量
    private String taskStatus;    // 任务状态
    private Date planStartDate;   // 计划开始日期
    private Date planEndDate;     // 计划结束日期
    private Date factStartDate;   // 实际开始日期
    private Date factEndDate;     // 实际结束日期
    
    // 订单相关字段
    private Integer orderPlanQuantity; // 订单计划数量
    private String orderStatus;        // 订单状态
    private String contractNum;        // 合同编号

}
