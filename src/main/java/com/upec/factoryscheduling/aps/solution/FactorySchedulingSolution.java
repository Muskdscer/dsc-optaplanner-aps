package com.upec.factoryscheduling.aps.solution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.ValidateSolution;
import com.upec.factoryscheduling.aps.entity.WorkCenter;
import lombok.Data;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工厂调度规划解决方案类
 * <p>这是OptaPlanner规划引擎的核心解决方案类，包含了调度问题的所有数据和规划结果：
 * - 规划实体（需要优化安排的工序时间槽）
 * - 问题事实（可用的工作中心、时间范围、维护计划等约束条件）
 * - 规划分数（评估解决方案质量的指标）
 * </p>
 */
@PlanningSolution  // 标记此类为OptaPlanner规划解决方案
@Data  // Lombok注解，自动生成getter、setter、equals、hashCode和toString方法
public class FactorySchedulingSolution {

    /**
     * 规划实体集合 - 需要被优化安排的时间槽列表
     * <p>每个Timeslot代表一个需要被分配到特定工作中心和开始时间的工序</p>
     */
    @PlanningEntityCollectionProperty  // 标记这是规划实体的集合，OptaPlanner将优化这些实体的规划变量
    private List<Timeslot> timeslots;

    /**
     * 时间范围 - 可用的时间点列表
     * <p>为规划变量提供可能的开始时间值范围</p>
     */
    @ValueRangeProvider(id = "timeslotRange")  // 提供时间点的取值范围，供规划变量选择
    @ProblemFactCollectionProperty  // 标记这是问题事实（约束条件）
    @JsonIgnore  // JSON序列化时忽略此字段
    private List<LocalDateTime> timeslotRange;

    /**
     * 设备维护计划列表 - 影响工作中心可用性的约束条件和规划变量取值范围
     * <p>在维护期间，对应的工作中心不可用</p>
     */
    @ValueRangeProvider(id = "maintenanceRange")  // 提供维护计划的取值范围，供规划变量选择
    @ProblemFactCollectionProperty  // 标记这是问题事实（约束条件）
    @JsonIgnore  // JSON序列化时忽略此字段
    private List<WorkCenterMaintenance> maintenances;
    /**
     * 规划分数 - 评估解决方案质量的指标
     * <p>使用HardSoftScore类型，包含硬约束和软约束的违反情况：
     * - 硬约束：必须满足的规则，如设备冲突、维护时间冲突等
     * - 软约束：应当尽量满足的规则，如订单优先级、完成时间等
     * </p>
     */
    @PlanningScore  // 标记这是规划分数字段，OptaPlanner将计算并设置此值
    private HardSoftScore score;

    /**
     * 求解器状态 - 表示当前规划过程的状态
     * <p>如NOT_SOLVING、SOLVING_ACTIVE、SOLVING_SCHEDULED等</p>
     */
    private SolverStatus solverStatus;

    /**
     * 默认构造函数
     * <p>为了序列化和框架要求而提供</p>
     */
    public FactorySchedulingSolution() {
    }

    /**
     * 创建工厂调度解决方案的构造函数（含维护计划）
     *
     * @param timeslots     需要安排的时间槽（工序）列表
     * @param timeslotRange 可用的时间范围列表
     * @param maintenances  工作中心维护计划列表
     */
    public FactorySchedulingSolution(List<Timeslot> timeslots,
                                     List<LocalDateTime> timeslotRange,
                                     List<WorkCenterMaintenance> maintenances) {
        this.timeslots = timeslots;
        this.timeslotRange = timeslotRange;
        this.maintenances = maintenances;
    }
}
