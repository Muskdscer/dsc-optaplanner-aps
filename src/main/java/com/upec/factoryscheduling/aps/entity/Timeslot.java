package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.aps.solution.TimeslotVariableListener;
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
@Getter
@Setter
@Data
@Table(name = "aps_timeslot")
public class Timeslot implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @PlanningId
    private String id;
    @Column(name = "problem_id")
    private Long problemId;

    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Procedure procedure;

    //该时间槽当天所需分配时间(分钟)
    private int duration;

    //优先级
    private Integer priority;

    //规划开始时间
    @ShadowVariable(variableListenerClass = TimeslotVariableListener.class, sourceVariableName = "maintenance", sourceEntityClass = Timeslot.class)
    @Column(name = "start_time")
    private LocalDateTime startTime;

    //绑定的工作中心日历
    @PlanningVariable(valueRangeProviderRefs = "maintenanceRange")
    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private WorkCenterMaintenance maintenance;

    //当前工序是否为并行工序
    private boolean parallel;

    //当天工序已完成或者手动排序,该时间槽不可动
    private boolean manual;

    //当前工序的时间槽索引
    @Column(name = "p_index")
    private int index;

    //当前工序的总时间槽个数
    private int total;

    //当前工序索引
    @Column(name = "procedure_index")
    private int procedureIndex;

    public LocalDateTime getEndTime() {
        if (this.startTime != null && this.duration >= 0) {
            return this.startTime.plusMinutes(this.duration);
        }
        return null;
    }
}
