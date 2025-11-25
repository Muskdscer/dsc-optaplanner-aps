package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.aps.solution.TimeslotVariableListener;
import com.upec.factoryscheduling.aps.solution.WorkCenterMaintenanceVariableListener;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import javax.persistence.*;
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

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Procedure procedure;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Order order;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Task task;

    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenter workCenter; // 从procedure中继承工作中心，不需要作为规划变量

    private double duration; // 使用double类型，支持0.5小时的颗粒度

    private Integer priority; // 优先级，直接存储计算好的优先级值

    @ShadowVariable(variableListenerClass = WorkCenterMaintenanceVariableListener.class, sourceVariableName =
            "maintenance")
    private LocalDateTime startTime; // 基于维护计划自动计算的开始时间

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "startTime")
    private LocalDateTime endTime; // 自动计算的结束时间

    @PlanningVariable(valueRangeProviderRefs = "maintenanceRange")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private WorkCenterMaintenance maintenance; // 维护计划作为规划变量，startTime将基于它自动计算

    private boolean parallel;

    private boolean manual;

    private Integer index; // 分片索引，用于标识同一工序的不同分片

    private Integer total; // 总分片数量

    private int procedureIndex;


}
