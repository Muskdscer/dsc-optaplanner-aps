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
    private double duration; // 改为double类型，支持0.5小时的颗粒度
    private Integer priority; // 优先级，直接存储计算好的优先级值

    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private LocalDateTime startTime;

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "startTime")
    private LocalDateTime endTime; // 自动计算的结束时间

    @OneToOne(fetch = FetchType.EAGER)
    @PlanningVariable(valueRangeProviderRefs = "maintenanceRange")
    private WorkCenterMaintenance maintenance; // 维护计划作为规划变量

    private boolean parallel;
    private boolean manual;
    private Integer index; // 分片索引，用于标识同一工序的不同分片
    private Integer total; // 总分片数量
}
