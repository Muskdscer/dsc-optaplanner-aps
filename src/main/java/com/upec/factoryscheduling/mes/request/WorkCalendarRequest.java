package com.upec.factoryscheduling.mes.request;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 工作日历创建请求实体类
 * 用于封装创建工作日历的请求参数
 */
public class WorkCalendarRequest {

    /**
     * 开始日期
     * 格式：yyyy-MM-dd
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     * 格式：yyyy-MM-dd
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 获取开始日期
     * @return 开始日期
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * 设置开始日期
     * @param startDate 开始日期
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * 获取结束日期
     * @return 结束日期
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * 设置结束日期
     * @param endDate 结束日期
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * 验证日期范围的有效性
     * @return 是否有效
     */
    public boolean isValidDateRange() {
        return !startDate.isAfter(endDate);
    }
}