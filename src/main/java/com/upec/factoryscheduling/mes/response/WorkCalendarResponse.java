package com.upec.factoryscheduling.mes.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 工作日历创建响应实体类
 * 用于封装创建工作日历的响应数据
 */
@Getter
@Setter
public class WorkCalendarResponse {
    
    /**
     * 创建的工作日历记录总数
     */
    private int totalCreated;
    
    /**
     * 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 结束日期
     */
    private LocalDate endDate;
    
    /**
     * 构建成功响应对象
     * @param totalCreated 创建的记录总数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 响应对象实例
     */
    public static WorkCalendarResponse success(int totalCreated, LocalDate startDate, LocalDate endDate) {
        WorkCalendarResponse response = new WorkCalendarResponse();
        response.setTotalCreated(totalCreated);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        return response;
    }
}
