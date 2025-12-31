package com.upec.factoryscheduling.mes.response;

import com.upec.factoryscheduling.common.utils.DateUtils;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private LocalDate planStartDate;   // 计划开始日期
    private LocalDate planEndDate;     // 计划结束日期
    private LocalDateTime factStartDate;   // 实际开始日期
    private LocalDateTime factEndDate;     // 实际结束日期

    // 订单相关字段
    private Integer orderPlanQuantity; // 订单计划数量
    private String orderStatus;        // 订单状态
    private String contractNum;        // 合同编号

    private String productCode;
    private String productName;

    private final String format = "yyyy-MM-dd HH:mm:ss";

    public void setFactEndDate(String factEndDate) {
        if (factEndDate != null) {
            this.factEndDate = DateUtils.parseDateTime(factEndDate, format);
        }
    }

    public void setFactStartDate(String factStartDate) {
        if (factStartDate != null) {

            this.factStartDate = DateUtils.parseDateTime(factStartDate, format);
        }
    }

    public void setPlanEndDate(String planEndDate) {
        this.planEndDate = DateUtils.parseLocalDate(planEndDate);
    }

    public void setPlanStartDate(String planStartDate) {
        this.planStartDate = DateUtils.parseLocalDate(planStartDate);
    }
}
