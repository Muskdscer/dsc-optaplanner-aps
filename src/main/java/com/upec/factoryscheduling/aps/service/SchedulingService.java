package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.*;
import com.upec.factoryscheduling.aps.response.TimeslotValidate;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.xkzhangsan.time.calculator.DateTimeCalculatorUtil;
import io.micrometer.core.ipc.http.HttpSender;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工厂调度服务类
 * <p>负责管理整个工厂订单和工序的调度过程，包括启动调度、停止调度、保存结果等核心功能。
 * 该服务整合了OptaPlanner求解器来解决复杂的工厂调度问题，确保资源优化利用和订单准时交付。</p>
 */
@Service
@Slf4j
public class SchedulingService {

    /**
     * 订单服务 - 负责订单相关的数据访问和业务逻辑
     */
    private OrderService orderService;

    /**
     * 工序服务 - 负责工序相关的数据访问和业务逻辑
     */
    private ProcedureService processService;

    /**
     * 工作中心服务 - 负责工作中心(设备/机器)相关的数据访问和业务逻辑
     */
    private WorkCenterService workCenterService;

    /**
     * 设备维护服务 - 负责设备维护计划相关的数据访问和业务逻辑
     */
    private WorkCenterMaintenanceService maintenanceService;

    /**
     * OptaPlanner求解器管理器 - 用于创建和管理求解作业
     */
    private SolverManager<FactorySchedulingSolution, Long> solverManager;

    /**
     * 解决方案管理器 - 用于更新和解释解决方案
     */
    private SolutionManager<FactorySchedulingSolution, HardMediumSoftScore> solutionManager;

    /**
     * 时间槽服务 - 负责时间槽相关的数据访问和业务逻辑
     */
    private TimeslotService timeslotService;


    @Autowired
    public void setTimeslotService(TimeslotService timeslotService) {
        this.timeslotService = timeslotService;
    }

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Autowired
    public void setProcessService(ProcedureService processService) {
        this.processService = processService;
    }

    @Autowired
    public void setMachineService(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }

    @Autowired
    public void setMaintenanceService(WorkCenterMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Autowired
    public void setSolverManager(SolverManager<FactorySchedulingSolution, Long> solverManager) {
        this.solverManager = solverManager;
    }

    @Autowired
    public void setSolutionManager(SolutionManager<FactorySchedulingSolution, HardMediumSoftScore> solutionManager) {
        this.solutionManager = solutionManager;
    }

    /**
     * 开始调度过程
     * <p>启动OptaPlanner求解器，根据指定的订单列表生成最优的调度方案。
     * 求解过程采用异步监听模式，实时获取并记录求解进度和结果。</p>
     *
     * @param problemId 问题ID - 用于标识和管理不同的调度问题实例
     * @param taskNos   订单编号列表 - 指定需要参与调度的订单，如果为空则调度所有订单
     */
    public void startScheduling(Long problemId, List<String> taskNos) {
        // 加载调度问题数据，包括订单、工序、时间槽等信息
        FactorySchedulingSolution problem = loadProblemWithSlices(taskNos, problemId);

        // 使用求解器管理器创建求解作业并监听进度
        SolverJob<FactorySchedulingSolution, Long> solverJob = solverManager.solveAndListen(
                problemId,  // 问题标识
                id -> problem,  // 提供问题数据的函数
                // 每次找到更好的解决方案时的回调函数
                solution -> {
                    // 记录新的最佳解决方案分数
                    log.info("New best solution found: {}", solution.getScore());
                    // 此处可以扩展，例如更新UI或临时保存中间结果
                },
                // 求解完成时的回调函数
                finalBestSolution -> {
                    // 记录最终最佳解决方案分数
                    log.info("Final best solution found: {}", finalBestSolution.getScore());
                    // 保存最终调度结果到数据库
                    saveSolution(finalBestSolution);
                },
                // 求解出错时的回调函数
                (id, throwable) -> {
                    log.error("Scheduling error: {}", throwable.getMessage());
                });
    }


    public List<TimeslotValidate> validateScheduling(List<String> taskNos) {
        List<Timeslot> timeslots = timeslotService.findAllByTaskIn(taskNos);
        List<TimeslotValidate> timeslotValidates = new ArrayList<>();
        for (Timeslot timeslot : timeslots) {
            if (timeslot.getWorkCenter() != null
                    && timeslot.getWorkCenter().getWorkCenterCode().equals("PM10W200")
                    && timeslot.getDuration() == 0) {
                TimeslotValidate timeslotValidate = new TimeslotValidate();
                timeslotValidate.setTaskNo(timeslot.getTask().getTaskNo());
                timeslotValidate.setProcedureId(timeslot.getProcedure().getId());
                timeslotValidate.setMessage("外协工序请先分配合理的外协时间!");
                timeslotValidates.add(timeslotValidate);
                continue;
            }
            if(timeslot.getWorkCenter() != null&&timeslot.getDuration()>480){
                TimeslotValidate timeslotValidate = new TimeslotValidate();
            }
        }
        return timeslotValidates;
    }


    /**
     * 停止调度过程
     * <p>提前终止正在进行的求解过程，获取当前的最佳解。
     * 该方法不会丢弃已计算的中间结果，可以通过getBestSolution获取当前最优解。</p>
     *
     * @param problemId 问题ID - 标识需要停止的调度问题实例
     */
    public void stopScheduling(Long problemId) {
        // 调用求解器管理器终止指定问题ID的求解过程
        solverManager.terminateEarly(problemId);
    }

    /**
     * 获取当前最佳解决方案
     * <p>返回指定问题ID的当前最优解，并设置其求解状态。
     * 无论求解是否完成，此方法都返回当前计算出的最佳解决方案。</p>
     *
     * @param problemId 问题ID - 标识需要获取解决方案的调度问题实例
     * @return FactorySchedulingSolution - 当前的最佳调度解决方案，包含求解状态信息
     */
    public FactorySchedulingSolution getBestSolution(Long problemId) {
        // 获取最终最佳解决方案
        FactorySchedulingSolution solution = getFinalBestSolution();
        // 获取并设置当前求解状态
        SolverStatus solverStatus = solverManager.getSolverStatus(problemId);
        solution.setSolverStatus(solverStatus);
        return solution;
    }


    /**
     * 获取解决方案得分
     * <p>返回当前最佳解决方案的得分，采用HardSoftScore形式，包含硬性约束分数和软性约束分数。
     * 硬性约束分数为负表示存在违反硬性约束的情况，解决方案不可行；
     * 软性约束分数越高表示解决方案越好。</p>
     *
     * @param problemId 问题ID - 标识需要获取得分的调度问题实例
     * @return HardMediumSoftScore - 当前最佳解决方案的评分对象
     */
    public HardMediumSoftScore getScore(Long problemId) {
        // 从最佳解决方案中提取得分
        return getBestSolution(problemId).getScore();
    }

    /**
     * 检查求解是否正在进行
     * <p>返回指定问题ID的求解器当前状态，包括：
     * NOT_SOLVING - 未开始求解或已停止
     * SOLVING_ACTIVE - 正在积极求解
     * SOLVING_SCHEDULED - 求解已安排但尚未开始</p>
     *
     * @param problemId 问题ID - 标识需要检查状态的调度问题实例
     * @return SolverStatus - 当前求解器的状态枚举值
     */
    public SolverStatus isSolving(Long problemId) {
        // 获取并返回求解器状态
        return solverManager.getSolverStatus(problemId);
    }

    /**
     * 加载调度问题数据
     * <p>根据指定的订单编号列表加载调度所需的所有数据，包括订单、工序、时间槽和设备维护计划等。
     * 此方法是调度问题求解的基础，负责构建初始的问题空间。</p>
     *
     * @param taskNos   订单编号列表 - 指定需要加载的订单，如果为空则加载所有订单
     * @param problemId 问题ID - 用于标识当前调度问题实例
     * @return FactorySchedulingSolution - 包含所有调度所需数据的问题实例
     */
    private FactorySchedulingSolution loadProblem(List<String> taskNos, Long problemId) {
        // 获取时间槽数据
        List<Timeslot> timeslots = new ArrayList<>();
        List<WorkCenter> workCenters = new ArrayList<>();
        // 查找与订单相关的所有时间槽并设置问题ID
        timeslots = timeslotService.findAllByTaskIn(taskNos).stream().peek(timeslot -> {
            if (timeslot.getWorkCenter() != null) {
                workCenters.add(timeslot.getWorkCenter());
            }
            timeslot.setProblemId(problemId);
            if (timeslot.getProcedure().getStartTime() != null) {
                timeslot.setStartTime(timeslot.getProcedure().getStartTime());
            }
            if (timeslot.getProcedure().getEndTime() != null) {
                timeslot.setEndTime(timeslot.getProcedure().getEndTime());
                timeslot.setManual(true);
            }
        }).filter(timeslot -> timeslot.getWorkCenter() != null).collect(Collectors.toList());
        // 获取设备维护计划
        List<WorkCenterMaintenance> maintenances = new ArrayList<>();
        // 确定时间范围（基于订单的计划开始和结束日期）
        LocalDate start = timeslots.stream().map(Timeslot::getOrder)
                .map(Order::getPlanStartDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate end = timeslots.stream().map(Timeslot::getOrder)
                .map(Order::getPlanEndDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        maintenances = maintenanceService.findAllByMachineInAndDateBetween(workCenters, start, end.plusDays(10));
        return new FactorySchedulingSolution(timeslots, maintenances);
    }


    /**
     * 加载带有工序分片的调度问题数据
     * <p>该方法专门处理已分片的工序数据，确保分片之间的连续性和约束关系。</p>
     *
     * @param orderNos  订单编号列表
     * @param problemId 问题ID
     * @return FactorySchedulingSolution - 包含所有调度所需数据的问题实例
     */
    private FactorySchedulingSolution loadProblemWithSlices(List<String> orderNos, Long problemId) {
        // 首先加载基础问题数据
        FactorySchedulingSolution solution = loadProblem(orderNos, problemId);
        // 如果有时间槽数据，对分片数据进行额外处理
        if (!CollectionUtils.isEmpty(solution.getTimeslots())) {
            // 按工序ID和分片索引对时间槽进行排序，确保分片顺序正确
            List<Timeslot> sortedTimeslots = solution.getTimeslots().stream()
                    .sorted((t1, t2) -> {
                        // 首先按工序ID排序
                        int procCompare = t1.getProcedure() != null && t2.getProcedure() != null ?
                                t1.getProcedure().getId().compareTo(t2.getProcedure().getId()) : 0;
                        if (procCompare != 0) return procCompare;

                        // 然后按分片索引排序
                        return Integer.compare(t1.getIndex(), t2.getIndex());
                    }).collect(Collectors.toList());

            // 设置分片之间的连接关系
            setupSliceRelationships(sortedTimeslots);

            // 更新解决方案中的时间槽列表
            solution.setTimeslots(sortedTimeslots);
        }

        return solution;
    }

    /**
     * 设置分片之间的关系，确保同一工序的分片按顺序连接
     */
    private void setupSliceRelationships(List<Timeslot> sortedTimeslots) {
        // 按工序ID分组
        Map<String, List<Timeslot>> timeslotsByProcedure = sortedTimeslots.stream()
                .filter(t -> t.getProcedure() != null)
                .collect(Collectors.groupingBy(t -> t.getProcedure().getId()));

        // 处理每个工序的分片关系
        timeslotsByProcedure.forEach((procedureId, procedureSlices) -> {
            if (procedureSlices.size() > 1) {
                // 对分片按索引排序
                procedureSlices.sort(Comparator.comparing(Timeslot::getIndex));

                // 设置前一个分片和后一个分片的关系
                for (int i = 0; i < procedureSlices.size(); i++) {
                    Timeslot currentSlice = procedureSlices.get(i);

                    // 设置前一个分片（如果有）
                    if (i > 0) {
                        Timeslot previousSlice = procedureSlices.get(i - 1);
                        // 这里可以添加额外的约束标记，表示前一个分片必须在当前分片之前完成
                    }

                    // 设置后一个分片（如果有）
                    if (i < procedureSlices.size() - 1) {
                        Timeslot nextSlice = procedureSlices.get(i + 1);
                        // 这里可以添加额外的约束标记，表示当前分片必须在后续分片之前完成
                    }
                }
            }
        });
    }

    /**
     * 保存调度结果
     * <p>将求解器生成的调度解决方案持久化到数据库中。
     * 主要包括保存所有时间槽信息，并根据时间槽更新对应工序的开始和结束时间。
     * 方法使用@Transactional注解确保数据一致性。</p>
     *
     * @param solution FactorySchedulingSolution - 求解器生成的调度解决方案
     */
    @Transactional("h2TransactionManager")
    public void saveSolution(FactorySchedulingSolution solution) {
        log.info("开始保存调度解决方案");

        if (solution == null) {
            log.warn("保存失败：解决方案对象为null");
            return;
        }
        if (CollectionUtils.isEmpty(solution.getTimeslots())) {
            log.warn("保存失败：解决方案中没有时间槽数据");
            return;
        }
        List<Timeslot> timeslots = solution.getTimeslots();
        int i = 1;
        for (Timeslot timeslot : timeslots) {
            if (timeslot.getMaintenance() == null) {
                continue;
            }
            if (!timeslot.getMaintenance().getWorkCenter().getWorkCenterCode().equals(timeslot.getWorkCenter().getWorkCenterCode())) {
                log.info("机器匹配,任务号:{},工序号:{},是否匹配:{},数量:{}", timeslot.getTask().getTaskNo(),
                        timeslot.getProcedure().getProcedureNo(),
                        false, i++);
            }
        }
        try {
            // 首先保存所有时间槽到数据库
            int savedCount = solution.getTimeslots().size();
            timeslotService.saveAll(solution.getTimeslots());
            log.info("已保存 {} 个时间槽到数据库", savedCount);
            // 根据工序ID对时间槽进行分组
            Map<String, List<Timeslot>> timeslotsByProcedure = solution.getTimeslots().stream()
                    .filter(t -> t.getProcedure() != null)  // 过滤出关联了工序的时间槽
                    .collect(Collectors.groupingBy(t -> t.getProcedure().getId()));
            log.info("找到 {} 个关联了工序的时间槽组", timeslotsByProcedure.size());
            log.info("调度解决方案保存完成");
        } catch (Exception e) {
            log.error("保存调度解决方案时发生错误：", e);
            throw e; // 重新抛出异常，让事务回滚
        }
    }

    /**
     * 根据时间槽信息更新工序的整体时间
     * <p>分析工序相关的所有时间槽，确定工序的实际开始时间和结束时间，并更新工序对象。
     * 工序的开始时间为其所有时间槽中的最早开始时间，结束时间为最晚结束时间。</p>
     *
     * @param procedure    工序对象 - 需要更新时间信息的工序
     * @param allTimeslots 时间槽列表 - 包含所有时间槽，需要筛选出与当前工序相关的
     */
    private void updateProcedureTimes(Procedure procedure, List<Timeslot> allTimeslots) {
        // 参数校验
        if (procedure == null || allTimeslots == null) {
            return;
        }
        // 筛选出与当前工序相关且有开始时间的时间槽
        List<Timeslot> procedureTimeslots = allTimeslots.stream()
                .filter(timeslot -> timeslot.getProcedure() != null && timeslot.getProcedure().equals(procedure))
                .filter(timeslot -> timeslot.getStartTime() != null)
                .collect(Collectors.toList());
        // 只有当存在相关时间槽时才更新工序时间
        if (!procedureTimeslots.isEmpty()) {
            // 找出最早的开始时间
            LocalDateTime earliestStart = procedureTimeslots.stream()
                    .map(Timeslot::getStartTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            // 找出最晚的结束时间
            LocalDateTime latestEnd = procedureTimeslots.stream()
                    .map(Timeslot::getEndTime)
                    .filter(Objects::nonNull)  // 过滤掉空的结束时间
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            // 更新工序的开始时间和开始日期
            if (processService != null) {
                procedure.setStartTime(earliestStart);
                procedure.setPlanStartDate(earliestStart.toLocalDate());
            }
            // 更新工序的结束时间和结束日期
            if (latestEnd != null && processService != null) {
                procedure.setEndTime(latestEnd);
                procedure.setPlanEndDate(latestEnd.toLocalDate());
            }
        }
    }

    /**
     * 保存调度结果（兼容方法）
     * <p>与saveSolution方法功能相同，为了向后兼容而保留。
     * 此方法在调度完成时由回调函数调用，负责持久化最终的调度方案。</p>
     *
     * @param solution FactorySchedulingSolution - 求解器生成的最终调度解决方案
     */
    public void saveSchedulingResult(FactorySchedulingSolution solution) { // 保留此方法以兼容现有调用
        if (solution != null && solution.getTimeslots() != null) {
            // 保存所有时间槽
            timeslotService.saveAll(solution.getTimeslots());
            // 更新工序的开始和结束时间
            Map<String, List<Timeslot>> timeslotsByProcedure = solution.getTimeslots().stream()
                    .filter(t -> t.getProcedure() != null)
                    .collect(Collectors.groupingBy(t -> t.getProcedure().getId()));

            timeslotsByProcedure.forEach((procedureId, timeslotList) -> {
                Procedure procedure = timeslotList.get(0).getProcedure();
                if (procedure != null) {
                    updateProcedureTimes(procedure, timeslotList);
                    if (processService != null) {
                        processService.saveProcedure(procedure);
                    }
                }
            });
        }
    }

    /**
     * 获取最终最佳解决方案
     * <p>从数据库中加载所有时间槽和维护记录，构建一个完整的解决方案对象。
     * 此方法主要用于获取当前系统中已保存的调度结果。</p>
     *
     * @return FactorySchedulingSolution - 包含所有已保存数据的解决方案对象
     */
    public FactorySchedulingSolution getFinalBestSolution() {
        // 初始化数据容器
        List<Timeslot> timeslots = new ArrayList<>();
        List<WorkCenterMaintenance> maintenances = new ArrayList<>();
        // 获取所有时间槽数据
        if (timeslotService != null) {
            // 直接从时间槽服务获取所有时间槽
            timeslots = timeslotService.findAll().getTimeslots();
        }
        // 获取所有维护记录
        if (maintenanceService != null) {
            maintenances = maintenanceService.getAllMaintenances();
        }
        // 使用正确的构造函数创建解决方案实例
        return new FactorySchedulingSolution(timeslots, maintenances);
    }

    /**
     * 验证解决方案是否可行
     * <p>检查当前最佳解决方案是否满足所有硬性约束（即是否可行）。
     * 如果解决方案的硬性约束分数为0或正数，则认为可行；如果为负数，则表示存在违反硬性约束的情况。</p>
     *
     * @param problemId 问题ID - 标识需要验证的调度问题实例
     * @return boolean - 如果解决方案可行返回true，否则返回false
     */
    public boolean isSolutionFeasible(Long problemId) {
        // 获取最佳解决方案的得分，并检查是否可行
        return getBestSolution(problemId).getScore().isFeasible();
    }

    /**
     * 更新问题数据
     * <p>当已有解决方案需要调整时，使用此方法更新问题数据。
     * 这允许在不重新开始整个求解过程的情况下，对当前解决方案进行修改。</p>
     *
     * @param problemId       问题ID - 标识需要更新的调度问题实例
     * @param updatedSolution 更新后的解决方案 - 包含最新的问题数据
     */
    public void updateProblem(Long problemId, FactorySchedulingSolution updatedSolution) {
        // 使用解决方案管理器更新问题数据
        solutionManager.update(updatedSolution);
    }

    /**
     * 获取解决方案的详细解释
     * <p>提供对当前最佳解决方案得分的详细分析，包括：
     * - 各个约束的满足情况
     * - 违反约束的具体原因
     * - 改进解决方案的建议
     * 这对于理解和优化调度结果非常有用。</p>
     *
     * @param problemId 问题ID - 标识需要解释的调度问题实例
     * @return ScoreExplanation - 包含解决方案得分详细解释的对象
     */
    public ScoreExplanation<FactorySchedulingSolution, HardMediumSoftScore> explainSolution(Long problemId) {
        // 获取最佳解决方案
        FactorySchedulingSolution solution = getBestSolution(problemId);
        // 使用解决方案管理器生成解释
        return solutionManager.explain(solution);
    }

    /**
     * 删除所有调度相关数据
     * <p>清空系统中的所有调度数据，包括时间槽、维护记录、工作中心、订单和工序信息。
     * 此方法主要用于测试或系统重置场景，谨慎使用。</p>
     */
    @Transactional("h2TransactionManager")
    public void delete() {
        // 按顺序删除所有相关数据
        timeslotService.deleteAll();
        maintenanceService.deleteAll();
        workCenterService.deleteAll();
        orderService.deleteAll();
        processService.deleteAll();
    }


    /**
     * 验证解决方案的约束满足情况
     * <p>对调度解决方案进行额外的业务规则验证，主要检查：
     * 1. 是否超出设备每日容量限制
     * 2. 设备使用时间是否存在重叠
     * 只验证手动设置的时间槽。</p>
     *
     * @param solution 需要验证的解决方案
     * @return 验证后的解决方案，包含验证结果信息
     */
    public FactorySchedulingSolution validation(FactorySchedulingSolution solution) {
        // 初始化验证结果列表
        List<ValidateSolution> validateSolutions = new ArrayList<>();
        List<Timeslot> timeslots = solution.getTimeslots();
        // 按设备和日期分组时间槽
        Map<String, List<Timeslot>> map = timeslots.stream()
                .collect(Collectors.groupingBy(timeslot ->
                        timeslot.getProcedure().getWorkCenterId().getWorkCenterCode() + "-" +
                                timeslot.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        // 遍历每个时间槽进行验证
        for (Timeslot timeslot : timeslots) {
            // 只验证手动设置的时间槽
            if (!timeslot.isManual()) {
                continue;
            }
            // 构建当前时间槽的设备-日期键
            String key = timeslot.getProcedure().getWorkCenterId().getWorkCenterCode() + "-" +
                    timeslot.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // 获取同一天同一设备的所有时间槽
            List<Timeslot> timeslotList = map.get(key);
            // 计算当日累计使用工时
            double countDailyHours = timeslotList.stream()
                    .mapToDouble(Timeslot::getDuration)
                    .sum();
            // 获取时间和设备信息
            LocalDateTime dateTime = timeslot.getStartTime();
            WorkCenter workCenter = timeslot.getProcedure().getWorkCenterId();
            // 获取设备当日的维护计划（包含容量信息）
            WorkCenterMaintenance maintenance = maintenanceService.findFirstByMachineAndDate(workCenter, dateTime.toLocalDate());
            // 创建验证结果对象
            ValidateSolution validateSolution = new ValidateSolution(
                    timeslot.getProcedure(),
                    timeslot.getOrder(),
                    timeslot.getMaintenance().getWorkCenter(),
                    maintenance);
            // 检查是否超出设备容量
            if (countDailyHours > maintenance.getCapacity()) {
                validateSolution.setMessage("超出当日机器容量!");
            }
            // 检查时间重叠
            long overlapTime = timeslotList.stream()
                    .mapToLong(t -> DateTimeCalculatorUtil.overlapTime(
                            timeslot.getStartTime().getMinute(),
                            timeslot.getStartTime().plusMinutes(timeslot.getDuration() * 60L).getMinute(),
                            t.getStartTime().getMinute(),
                            t.getStartTime().plusMinutes(t.getStartTime().getMinute()).getMinute()))
                    .sum();
            if (overlapTime > 0) {
                validateSolution.setMessage("当日机器使用时间有重叠部分");
            }
            // 添加到验证结果列表
            validateSolutions.add(validateSolution);
            // 更新时间槽的维护计划信息
            timeslot.setMaintenance(maintenance);
        }
        // 更新解决方案对象
        solution.setTimeslots(timeslots);
        return solution;
    }
}
