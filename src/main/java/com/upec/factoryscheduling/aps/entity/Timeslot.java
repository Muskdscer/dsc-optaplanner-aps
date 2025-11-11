package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.aps.solution.TimeslotVariableListener;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@PlanningEntity
@Data
@Setter
@Getter
public class Timeslot implements Serializable {
    @Id
    @PlanningId
    private String id;

    private Long problemId;

    @OneToOne(fetch = FetchType.EAGER)
    private Procedure procedure;

    @OneToOne(fetch = FetchType.EAGER)
    private Order order;

    @OneToOne(fetch = FetchType.EAGER)
    private Task task;

    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenter workCenter; // 从procedure中继承工作中心，不需要作为规划变量

    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private LocalDateTime startTime;
    
    private double duration; // 改为double类型，支持0.5小时的颗粒度
    
    private Integer sliceIndex; // 分片索引，用于标识同一工序的不同分片
    private Integer totalSlices; // 总分片数量
    
    private Integer priority; // 优先级，直接存储计算好的优先级值

    @OneToOne(fetch = FetchType.EAGER)
    @PlanningVariable(valueRangeProviderRefs = "maintenanceRange")
    private WorkCenterMaintenance maintenance; // 维护计划作为规划变量
    
    // 通过duration字段计算分钟数，保持与现有代码的兼容性
    public int getDailyHours() {
        // 将小时转换为分钟
        return (int) (duration * 60);
    }

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "startTime")
    private LocalDateTime endTime; // 自动计算的结束时间

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "startTime")
    private LocalDateTime dateTime;

    private boolean isManual;
    
    // 用于工序顺序约束
    private Integer procedureOrder; // 工序序号
}
