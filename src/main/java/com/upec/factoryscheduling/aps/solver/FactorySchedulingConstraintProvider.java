package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.extern.slf4j.Slf4j;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * 工厂调度约束提供器
 * <p>实现OptaPlanner的ConstraintProvider接口，定义了工厂调度问题中的所有约束条件：</p>
 * <ul>
 *   <li>硬约束（Hard Constraints）：必须满足的规则，违反会导致解决方案不可行</li>
 *   <li>软约束（Soft Constraints）：应当尽量满足的规则，违反会降低解决方案质量</li>
 * </ul>
 * <p>这些约束条件共同构成了工厂调度优化的评估体系，指导OptaPlanner找到最佳调度方案。</p>
 */
@Slf4j  // Lombok注解，提供日志记录功能
@Component  // Spring组件，使此类可被自动注入
public class FactorySchedulingConstraintProvider implements ConstraintProvider {
    /**
     * 定义所有调度约束条件
     * <p>返回约束数组，包含系统中所有的调度规则，分为以下几类：</p>
     * <ul>
     *   <li>核心约束：保证调度的基本可行性</li>
     *   <li>资源约束：处理设备维护等资源限制</li>
     *   <li>优化约束：提高调度方案的整体质量</li>
     *   <li>分片约束：处理工序分片执行的特殊规则</li>
     * </ul>
     * 
     * @param constraintFactory OptaPlanner提供的约束工厂，用于构建约束条件
     * @return 约束数组，包含所有定义的调度规则
     */
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // 核心约束 - 必须严格遵守
                workCenterConflict(constraintFactory),        // 工作中心时间冲突约束
                procedureSequenceConstraint(constraintFactory), // 工序顺序约束
                
                // 资源约束 - 处理资源可用性限制
                workCenterMaintenanceConflict(constraintFactory), // 工作中心维护冲突约束
                fixedStartTimeConstraint(constraintFactory), // 固定开始时间约束
                
                // 优化约束 - 提高解决方案质量
                maximizeOrderPriority(constraintFactory),      // 订单优先级最大化约束
                maximizeMachineUtilization(constraintFactory),  // 机器利用率最大化约束
                minimizeMakespan(constraintFactory),           // 制造周期最小化约束
                
                // 分片相关约束 - 处理工序分片执行的特殊规则
                procedureSliceSequence(constraintFactory),     // 工序分片顺序约束
                procedureSlicePreferContinuous(constraintFactory), // 工序分片连续性偏好约束
                orderStartDateProximity(constraintFactory)    // 订单开始时间接近度约束
        };
    }
    
    /**
     * 固定开始时间约束（硬约束）
     * <p>确保：</p>
     * <ul>
     *   <li>当Order的factStartDate不为空时，必须作为规划开始时间</li>
     *   <li>当Procedure的startTime不为空时，必须作为工序规划的开始时间</li>
     *   <li>当Procedure的endTime不为空时，表明工序已完成，不需要继续规划</li>
     * </ul>
     */
    private Constraint fixedStartTimeConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                // 跳过手动设置的时间槽，它们已经设置了固定的开始和结束时间
                .filter(timeslot -> !timeslot.isManual())
                .filter(timeslot -> timeslot.getStartTime() != null && timeslot.getProcedure() != null)
                .filter(timeslot -> {
                    // 检查工序是否已完成（endTime不为空），如果是则违反约束
                    if (timeslot.getProcedure().getEndTime() != null) {
                        return true; // 已完成的工序不应再被规划
                    }
                    // 检查Procedure的startTime是否不为空且与时间槽开始时间不同
                    if (timeslot.getProcedure().getStartTime() != null &&
                            !timeslot.getStartTime().equals(timeslot.getProcedure().getStartTime())) {
                        return true; // 必须使用固定的开始时间
                    }
                    // 检查Order的factStartDate是否不为空且与时间槽开始时间不同
                    if (timeslot.getOrder() != null && timeslot.getOrder().getFactStartDate() != null &&
                            !timeslot.getStartTime().equals(timeslot.getOrder().getFactStartDate())) {
                        return true; // 必须使用订单的实际开始时间
                    }
                    return false; // 没有违反约束
                }).penalize("Fixed start time violation", HardSoftScore.ONE_HARD);
    }
    
    /**
     * 订单开始时间接近度约束（软约束）
     * 确保规划时间尽量接近Order的planStartDate
     * planStartDate默认非空，规划应尽量接近开始时间
     */
    private Constraint orderStartDateProximity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                // 跳过手动设置的时间槽，它们不需要优化开始时间
                .filter(timeslot -> !timeslot.isManual())
                .filter(timeslot -> timeslot.getOrder() != null && timeslot.getStartTime() != null &&
                        timeslot.getOrder().getPlanStartDate() != null && timeslot.getIndex() == 0)
                .penalize("Order start date proximity", HardSoftScore.ofSoft(1),
                        timeslot -> {
                            // 计算时间槽开始时间与订单计划开始时间的差异（分钟）
                            // 将LocalDate转换为LocalDateTime进行比较
                            LocalDateTime planStartDateTime = LocalDateTime.of(
                                    timeslot.getOrder().getPlanStartDate(),
                                    LocalTime.MIN);
                            long minutesDiff = ChronoUnit.MINUTES.between(
                                    planStartDateTime,
                                    timeslot.getStartTime());
                            // 只惩罚比计划时间晚的情况，不惩罚提前开始
                            return Math.max(0, (int)minutesDiff);
                        });
    }

    /**
     * 工作中心冲突约束 - 硬约束
     * <p>确保同一工作中心在同一时间不能处理多个任务，防止资源冲突。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配工作中心和开始时间的时间槽</li>
     *   <li>按工作中心分组并检查时间重叠</li>
     *   <li>避免重复计数（通过ID比较确保只检查一次）</li>
     *   <li>对冲突情况应用硬约束惩罚</li>
     * </ol>
     * 
     * @param constraintFactory 约束工厂
     * @return 工作中心冲突约束对象
     */
    private Constraint workCenterConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getStartTime() != null && timeslot.getEndTime() != null)
                .join(Timeslot.class, 
                        Joiners.equal(Timeslot::getWorkCenter),
                        Joiners.lessThan(Timeslot::getId), // 避免重复计数
                        Joiners.filtering((a, b) -> {
                            if (a.getStartTime() == null || b.getStartTime() == null || a.getEndTime() == null || b.getEndTime() == null) {
                                return false;
                            }
                            // 检查时间重叠：如果两个时间区间有重叠，则存在冲突
                            return !a.getEndTime().isBefore(b.getStartTime()) && !b.getEndTime().isBefore(a.getStartTime());
                        }))
                .penalize(HardSoftScore.ONE_HARD).asConstraint( "Work center conflict");
    }
    
    /**
     * 工序顺序约束 - 硬约束
     * <p>确保有前后依赖关系的工序按正确顺序执行，防止工序执行顺序错误。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配工序且有后续工序的时间槽（仅最后一个分片）</li>
     *   <li>利用Procedure的nextProcedure链表结构连接前序和后续工序</li>
     *   <li>检查后续工序是否在前置工序完成前开始</li>
     *   <li>对违反顺序的情况应用硬约束惩罚</li>
     * </ol>
     * 
     * @param constraintFactory 约束工厂
     * @return 工序顺序约束对象
     */
    private Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getProcedure() != null && 
                        timeslot.getEndTime() != null && 
                        timeslot.getIndex() != null &&
                        timeslot.getTotal() != null &&
                        timeslot.getIndex().equals(timeslot.getTotal() - 1) && // 只考虑最后一个分片
                        timeslot.getProcedure().getNextProcedure() != null && !timeslot.getProcedure().getNextProcedure().isEmpty())
                .join(Timeslot.class, 
                        Joiners.filtering((prev, next) -> {
                            if (prev.getProcedure() == null || next.getProcedure() == null || 
                                next.getStartTime() == null || prev.getEndTime() == null || 
                                next.getTotal() == null) {
                                return false;
                            }
                            // 检查当前timeslot是否是后续工序的第一个分片
                            boolean isFirstSliceOfNextProcedure = next.getIndex() == 0;
                            // 检查next的procedure是否在prev的procedure的nextProcedure列表中
                            boolean isNextProcedure = prev.getProcedure().getNextProcedure().stream()
                                    .anyMatch(p -> p.getId().equals(next.getProcedure().getId()));
                            
                            // 只有当这两个条件都满足时，才应用约束
                            return isFirstSliceOfNextProcedure && isNextProcedure;
                        }))
                .filter((prev, next) -> next.getStartTime().isBefore(prev.getEndTime()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Procedure sequence constraint");
    }
    
    /**
     * 工作中心维护冲突约束 - 硬约束
     * <p>确保工序不会安排在工作中心维护期间或超出工作中心容量，考虑工作中心状态和使用时间。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>工作中心状态为'n'表示维护中，状态为'y'表示可用</li>
     *   <li>startTime和endTime表示工作中心可以工作的时间段</li>
     *   <li>capacity为通过开始时间和结束时间计算出的最大工作时长（分钟）</li>
     *   <li>usageTime为已使用时间，会根据规划动态变更</li>
     * </ol>
     * 
     * @param constraintFactory 约束工厂
     * @return 工作中心维护冲突约束对象
     */
    private Constraint workCenterMaintenanceConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getStartTime() != null && timeslot.getEndTime() != null)
                .join(WorkCenterMaintenance.class, 
                        Joiners.equal(Timeslot::getWorkCenter, WorkCenterMaintenance::getWorkCenter),
                        Joiners.filtering((timeslot, maintenance) -> {
                            if (timeslot.getStartTime() == null || timeslot.getEndTime() == null || 
                                maintenance.getStartTime() == null || maintenance.getEndTime() == null ||
                                timeslot.getStartTime().toLocalDate() != maintenance.getDate()) {
                                return false;
                            }
                            // 状态为'n'表示维护中，不能安排工序
                            if ("n".equals(maintenance.getStatus())) {
                                return true; // 硬约束违反
                            }
                            // 只有当状态为'y'或null时才检查时间范围
                            if (!"y".equals(maintenance.getStatus()) && maintenance.getStatus() != null) {
                                return false; // 其他状态不处理
                            }
                            // 将维护时间转换为与timeslot相同日期的LocalDateTime进行比较
                            LocalDateTime maintenanceStartDateTime = LocalDateTime.of(
                                    maintenance.getDate(), 
                                    maintenance.getStartTime());
                            LocalDateTime maintenanceEndDateTime = LocalDateTime.of(
                                    maintenance.getDate(), 
                                    maintenance.getEndTime());
                            // 检查工序是否完全在允许工作的时间范围内
                            // 如果工序开始时间早于维护允许时间，或结束时间晚于维护允许时间，则冲突
                            return timeslot.getStartTime().isBefore(maintenanceStartDateTime) || 
                                   timeslot.getEndTime().isAfter(maintenanceEndDateTime);
                        }))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Work center maintenance conflict");
    }
    
    /**
     * 订单优先级约束 - 软约束
     * <p>优先安排高优先级订单的任务，提高客户满意度和业务响应性。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配订单和开始时间的时间槽</li>
     *   <li>根据订单优先级给予奖励（优先级数值越小，奖励越高）</li>
     *   <li>使用较高权重（10）确保优先级得到充分重视</li>
     * </ol>
     * <p><strong>优先级计算：</strong>优先级1（最高）给予100奖励，优先级10给予10奖励，确保高优先级订单得到优先处理。</p>
     * 
     * @param constraintFactory 约束工厂
     * @return 订单优先级最大化约束对象
     */
    private Constraint maximizeOrderPriority(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                // 跳过手动设置的时间槽，它们不需要优先级优化
                .filter(timeslot -> !timeslot.isManual())
                .filter(timeslot -> timeslot.getOrder() != null && timeslot.getStartTime() != null && 
                        timeslot.getPriority() != null && timeslot.getPriority() <= 100)
                .reward(HardSoftScore.ofSoft(10), // 权重较高，确保优先级得到重视
                        // 奖励函数：将优先级数值转换为奖励值
                        // 优先级数值越大，优先级越高，给予更高奖励
                        // 公式：直接使用priority值，确保优先级100给予最大奖励
                        // 优先级默认为0，确保即使没有设置优先级也能正常计算
                        Timeslot::getPriority)
                .asConstraint("Maximize order priority");
    }
    
    /**
     * 机器利用率最大化约束 - 软约束
     * <p>鼓励充分利用设备资源，避免资源浪费，基于工作中心的可用容量计算。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配工作中心和开始时间的时间槽</li>
     *   <li>按工作中心和日期分组，计算每个组的总工作时间（分钟）</li>
     *   <li>利用WorkCenterMaintenance中的capacity作为该工作中心当天的总工作容量</li>
     *   <li>根据实际利用率（已用时间/总容量）给予相应奖励</li>
     * </ol>
     * <p><strong>目标：</strong>最大化机器利用率，使设备使用效率接近其可用容量。</p>
     * 
     * @param constraintFactory 约束工厂
     * @return 机器利用率最大化约束对象
     */
    private Constraint maximizeMachineUtilization(ConstraintFactory constraintFactory) {
        // 首先，我们需要获取所有WorkCenterMaintenance记录，按工作中心和日期分组
        // 然后，将这些信息与时间槽的分组信息结合，计算实际利用率
        return constraintFactory.forEach(WorkCenterMaintenance.class)
                .filter(maintenance -> maintenance.getWorkCenter() != null && maintenance.getDate() != null)
                // 为每个维护记录，找到当天该工作中心的所有时间槽
                .join(
                        Timeslot.class,
                        Joiners.equal(WorkCenterMaintenance::getWorkCenter, Timeslot::getWorkCenter),
                        Joiners.equal(WorkCenterMaintenance::getDate, timeslot -> timeslot.getStartTime() != null ? timeslot.getStartTime().toLocalDate() : null)
                )
                // 使用filter而不是Joiners.filtering
                .filter((maintenance, timeslot) -> timeslot.getStartTime() != null && timeslot.getWorkCenter() != null)
                // 按工作中心和日期分组，计算总工作时间和获取工作容量
                .groupBy(
                        (maintenance, timeslot) -> maintenance.getWorkCenter(),
                        (maintenance, timeslot) -> maintenance.getDate(),
                        (maintenance, timeslot) -> maintenance.getCapacity(), // 工作中心当天的容量（分钟）
                        ConstraintCollectors.sum((maintenance, timeslot) -> {
                            // 显式类型转换避免类型推断问题
                            Timeslot ts = (Timeslot) timeslot;
                            return (int) (ts.getDuration() * 60);
                        }) // 计算当天实际使用的分钟数
                )
                // 奖励函数：基于实际利用率给予奖励
                .reward(HardSoftScore.ofSoft(1),
                        (workCenter, date, capacity, usedMinutes) -> {
                            // 确保容量为正数，避免除以零
                            if (capacity > 0) {
                                // 计算利用率百分比（0-100%），然后映射到奖励值
                                // 使用平方根函数使得利用率越高，奖励增长越快
                                double utilizationRate = Math.min(1.0, (double) usedMinutes / capacity);
                                // 奖励值为容量乘以利用率的平方根，这样高利用率能获得更高的奖励
                                return (int) (capacity * Math.sqrt(utilizationRate));
                            }
                            // 如果没有维护记录或容量为零，使用默认奖励
                            return Math.min(480, usedMinutes); // 默认每天480分钟（8小时）
                        }
                )
                .asConstraint("Maximize machine utilization");
    }
    
    /**
     * 最小化制造周期约束 - 软约束
     * <p>尽量缩短整体制造周期，使任务尽早开始和完成，提高整体生产效率。</p>
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>筛选出已分配开始时间的时间槽</li>
     *   <li>计算从当前时间到任务开始时间的分钟数</li>
     *   <li>对开始时间越晚的任务应用越大的惩罚</li>
     *   <li>每延迟30分钟增加1点惩罚</li>
     * </ol>
     * <p><strong>目标：</strong>鼓励尽早安排任务，避免不必要的延迟。</p>
     * 
     * @param constraintFactory 约束工厂
     * @return 制造周期最小化约束对象
     */
    private Constraint minimizeMakespan(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null)
                // 惩罚函数：根据任务开始时间的延迟程度给予惩罚
                .penalize(HardSoftScore.ONE_SOFT, timeslot -> {
                    // 计算从当前时间到任务开始时间的分钟数
                    long minutesFromNow = LocalDateTime.now().until(timeslot.getStartTime(), ChronoUnit.MINUTES);
                    // 每30分钟的延迟增加1点惩罚
                    // Math.max(0, ...)确保不会对过去时间给予奖励
                    return (int) Math.max(0, minutesFromNow / 30);
                })
                .asConstraint("Minimize makespan");
    }
    
    /**
     * 工序分片顺序约束
     * 确保工序之间的依赖关系正确，上一道工序的最后一个分片必须在后续工序的第一个分片之前完成
     * 使用Procedure的nextProcedure链表结构替代nextProcedureId字段
     */
    /**
     * 工序分片时间范围约束（硬约束）
     * 确保分片时间在procedure的计划开始时间和结束时间之间
     * 这些计划时间会根据规划实时动态变更
     */
    private Constraint procedureSliceSequence(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                // 跳过手动设置的时间槽，它们不需要参与规划
                .filter(timeslot -> !timeslot.isManual())
                .filter(timeslot -> timeslot.getProcedure() != null && timeslot.getStartTime() != null && timeslot.getEndTime() != null)
                .filter(timeslot -> {
                    // 检查分片是否在procedure的计划时间范围内
                    boolean withinPlanTime = true;
                    
                    // 检查是否早于计划开始时间
                    if (timeslot.getProcedure().getPlanStartDate() != null) {
                        // 转换为相同类型进行比较
                        LocalDateTime planStartDateTime = LocalDateTime.of(
                                timeslot.getProcedure().getPlanStartDate(), 
                                LocalTime.MIN);
                        if (timeslot.getStartTime().isBefore(planStartDateTime)) {
                            withinPlanTime = false;
                        }
                    }
                    
                    // 检查是否晚于计划结束时间
                    if (timeslot.getProcedure().getPlanEndDate() != null) {
                        // 转换为相同类型进行比较
                        LocalDateTime planEndDateTime = LocalDateTime.of(
                                timeslot.getProcedure().getPlanEndDate(), 
                                LocalTime.MAX);
                        if (timeslot.getEndTime().isAfter(planEndDateTime)) {
                            withinPlanTime = false;
                        }
                    }
                    
                    return !withinPlanTime;
                })
                .penalize("Procedure slice time range", HardSoftScore.ONE_HARD);
    }

    /**
     * 工序分片连续性优化（软约束）
     * 优先让同一工序的分片连续执行，减少时间间隔
     */
    private Constraint procedureSlicePreferContinuous(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getIndex() != null && timeslot.getIndex() > 0 &&
                                   timeslot.getIndex() != null && timeslot.getIndex() > 1 &&
                                   timeslot.getStartTime() != null && timeslot.getProcedure() != null)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getProcedure() != null ? t.getProcedure().getId() : null, 
                                     t -> t.getProcedure() != null ? t.getProcedure().getId() : null),
                        Joiners.equal(t -> t.getIndex() != null ? t.getIndex() - 1 : -1, Timeslot::getIndex))
                .filter((current, previous) -> previous.getEndTime() != null && current.getStartTime() != null && 
                                               !current.getStartTime().equals(previous.getEndTime()))
                .penalize("Procedure slice prefer continuous", HardSoftScore.ONE_SOFT, (current, previous) -> {
                    if (previous.getEndTime() != null && current.getStartTime() != null) {
                        long gapMinutes = ChronoUnit.MINUTES.between(previous.getEndTime(), current.getStartTime());
                        return Math.max(0, (int)(gapMinutes / 30)); // 每30分钟一个惩罚点，转换为int类型
                    }
                    return 0;
                });
    }
}
