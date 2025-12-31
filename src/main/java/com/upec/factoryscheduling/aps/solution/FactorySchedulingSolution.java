package com.upec.factoryscheduling.aps.solution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 工厂调度规划解决方案类
 * <p>这是OptaPlanner规划引擎的核心解决方案类，包含了调度问题的所有数据和规划结果：
 * - 规划实体（需要优化安排的工序时间槽）
 * - 问题事实（可用的工作中心、时间范围、维护计划等约束条件）
 * - 规划分数（评估解决方案质量的指标）
 * </p>
 */
@PlanningSolution  // 标记此类为OptaPlanner规划解决方案
public class FactorySchedulingSolution implements  Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private Long problemId;


    /**
     * 时间槽（工序）列表 - 规划实体集合
     */
    @Getter
    @PlanningEntityCollectionProperty
    private List<Timeslot> timeslots;


    /**
     * 设备维护计划列表 - 影响工作中心可用性的约束条件和规划变量取值范围
     * <p>在维护期间，对应的工作中心不可用</p>
     * 使用CopyOnWriteArrayList确保线程安全的读取操作
     */
    @JsonIgnore
    @Getter
    @ValueRangeProvider(id = "maintenanceRange")
    @ProblemFactCollectionProperty
    private List<WorkCenterMaintenance> maintenances;

    /**
     * 规划分数 - 评估解决方案质量的指标
     * <p>使用HardSoftScore类型，包含硬约束和软约束的违反情况：
     * - 硬约束：必须满足的规则，如设备冲突、维护时间冲突等
     * - 软约束：应当尽量满足的规则，如订单优先级、完成时间等
     * </p>
     * volatile确保多线程环境下的可见性
     */
    @PlanningScore
    private volatile HardMediumSoftScore score;

    /**
     * 求解器状态 - 表示当前规划过程的状态
     * <p>如NOT_SOLVING、SOLVING_ACTIVE、SOLVING_SCHEDULED等</p>
     * volatile确保多线程环境下的可见性
     */
    private volatile SolverStatus solverStatus;
    
    /**
     * 获取规划分数
     */
    public synchronized HardMediumSoftScore getScore() {
        return score;
    }
    
    /**
     * 设置规划分数
     */
    public synchronized void setScore(HardMediumSoftScore score) {
        this.score = score;
    }
    
    /**
     * 获取求解器状态
     */
    public synchronized SolverStatus getSolverStatus() {
        return solverStatus;
    }
    
    /**
     * 设置求解器状态
     */
    public synchronized void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }

    /**
     * 默认构造函数
     * <p>为了序列化和框架要求而提供</p>
     */
    public FactorySchedulingSolution() {
        // 初始化线程安全的列表以避免空指针异常和确保并发安全
        this.timeslots = new CopyOnWriteArrayList<>();
        this.maintenances = new CopyOnWriteArrayList<>();
    }

    /**
     * 创建工厂调度解决方案的构造函数（含维护计划）
     *
     * @param timeslots     需要安排的时间槽（工序）列表
     * @param maintenances  工作中心维护计划列表
     */
    public FactorySchedulingSolution(List<Timeslot> timeslots,
                                     List<WorkCenterMaintenance> maintenances) {
        // 使用CopyOnWriteArrayList确保线程安全
        this.timeslots = timeslots != null ? new CopyOnWriteArrayList<>(timeslots) : new CopyOnWriteArrayList<>();
        this.maintenances = maintenances != null ? new CopyOnWriteArrayList<>(maintenances) : new CopyOnWriteArrayList<>();
    }

    /**
     * 线程安全地设置时间槽列表
     * @param timeslots 新的时间槽列表
     */
    public synchronized void setTimeslots(List<Timeslot> timeslots) {
        this.timeslots = timeslots != null ? new CopyOnWriteArrayList<>(timeslots) : new CopyOnWriteArrayList<>();
    }

    /**
     * 线程安全地设置维护计划列表
     * @param maintenances 新的维护计划列表
     */
    public synchronized void setMaintenances(List<WorkCenterMaintenance> maintenances) {
        this.maintenances = maintenances != null ? new CopyOnWriteArrayList<>(maintenances) : new CopyOnWriteArrayList<>();
    }

    /**
     * 线程安全地添加单个时间槽
     * @param timeslot 要添加的时间槽
     */
    public synchronized void addTimeslot(Timeslot timeslot) {
        if (timeslot != null) {
            this.timeslots.add(timeslot);
        }
    }

    /**
     * 线程安全地添加单个维护计划
     * @param maintenance 要添加的维护计划
     */
    public synchronized void addMaintenance(WorkCenterMaintenance maintenance) {
        if (maintenance != null) {
            this.maintenances.add(maintenance);
        }
    }

    /**
     * 线程安全地移除时间槽
     * @param timeslot 要移除的时间槽
     * @return 是否成功移除
     */
    public synchronized boolean removeTimeslot(Timeslot timeslot) {
        return timeslot != null && this.timeslots.remove(timeslot);
    }

    /**
     * 线程安全地移除维护计划
     * @param maintenance 要移除的维护计划
     * @return 是否成功移除
     */
    public synchronized boolean removeMaintenance(WorkCenterMaintenance maintenance) {
        return maintenance != null && this.maintenances.remove(maintenance);
    }


}
