package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.aps.solution.TimeslotVariableListener;
import com.upec.factoryscheduling.aps.solution.WorkCenterMaintenanceVariableListener;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private BigDecimal duration; // 改为double类型，支持0.5小时的颗粒度
    private Integer priority; // 优先级，直接存储计算好的优先级值

    @ShadowVariable(variableListenerClass = WorkCenterMaintenanceVariableListener.class, sourceVariableName = "maintenance")
    private LocalDateTime startTime; // 基于维护计划自动计算的开始时间

    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "startTime")
    private LocalDateTime endTime; // 自动计算的结束时间

    @PlanningVariable(valueRangeProviderRefs = "maintenanceRange")
    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenterMaintenance maintenance; // 维护计划作为规划变量，startTime将基于它自动计算

    /**
     * 个性化的维护计划取值范围，只包含与当前Timeslot的WorkCenter匹配的维护计划
     * 用于减少搜索空间，提高规划效率
     * -- SETTER --
     *  设置可用的维护计划列表（由外部调用，通常在规划开始前）
     *
     * @param availableMaintenances 与当前WorkCenter匹配的维护计划列表

     */
    @Transient
    private List<WorkCenterMaintenance> availableMaintenances;

    private boolean parallel;
    private boolean manual;
    private Integer index; // 分片索引，用于标识同一工序的不同分片
    private Integer total; // 总分片数量

    /**
     * 为当前Timeslot提供个性化的维护计划取值范围
     * 只返回与当前WorkCenter匹配的维护计划，减少搜索空间
     * @return 可用的维护计划列表
     */
    @ValueRangeProvider(id = "maintenanceRange")
    public List<WorkCenterMaintenance> getAvailableMaintenances() {
        return availableMaintenances != null ? availableMaintenances : new ArrayList<>();
    }

}
