# OptaPlanner 在APS系统使用流程
    基于 Spring Boot 和 OptaPlanner 的高级计划与调度系统，用于解决复杂的药厂生产调度问题。

## 系统概述
利用 OptaPlanner 的约束求解能力，为药厂生产提供智能调度解决方案。系统可以考虑了订单优先级、设备可用性、工序依赖关系等多种约束条件，生成最优的生产排期计划。

### 核心模块

1. **aps模块**：高级计划与调度核心功能
    - entity: 核心实体类（订单、工序、工作中心、时间槽等）
    - repository: 数据访问层
    - service: 业务逻辑层
    - controller: REST API接口
    - solution: OptaPlanner解决方案相关类
    - 
2**solver模块**：调度求解器
   - 定义调度约束条件和优化目标

### 核心实体关系

- **订单(Order)**：包含多个工序(Procedure)，有优先级属性
  - 
- **工序(Procedure)**：属于某个订单，必须在特定工作中心(WorkCenter)上执行，有顺序依赖关系
  - //所属订单
    private Order order;

    //所属任务
    private Task task;

    //所属工作中心(设备)
    private WorkCenter workCenter;
  
    //后面工序
    private List<Procedure> nextProcedure;
- **工作中心(WorkCenter)**：表示生产设备，有可用状态
  - private String workCenterCode;
    private String name;
    private String status;
- **时间槽(Timeslot)**：调度的基本单元，关联工序、工作中心和时间安排
  - //所属的工序
    private Procedure procedure;

    //该时间槽当天所需分配时间(分钟)
    private int duration;

    //优先级
    private Integer priority;
- **工作中心维护(WorkCenterMaintenance)**：表示设备的维护计划，维护期间设备不可用

## 调度约束说明

可以设计以下主要约束：约束类 ConstraintProvider

### 硬约束（必须满足）  HardMediumSoftScore.ONE_HARD
- 硬约束1: 工作中心必须匹配
- 违反条件：为维护任务分配了错误的工作中心
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot ->
    timeslot.getMaintenance() != null
    && timeslot.getProcedure().getWorkCenter() != null
    && !timeslot.getMaintenance().getWorkCenter().getId().equals(timeslot.getProcedure().getWorkCenter().getId()))
    .penalize(HardMediumSoftScore.ONE_HARD, timeslot -> HARD_PENALTY_WEIGHT * 10) // 严重违反
    .asConstraint("硬约束：工作中心必须匹配");
- 硬约束2: 不能超过维护容量
- 违反条件：分配给某天维护的任务总时长超过维护容量
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot -> timeslot.getMaintenance() != null && timeslot.getDuration() > 0 &&
    (timeslot.getProcedure().getWorkCenter() == null || !WORK_CENTER_CODE.equals(timeslot.getProcedure().getWorkCenter().getWorkCenterCode())))
    .groupBy(Timeslot::getMaintenance, sum(Timeslot::getDuration))
    .filter((maintenance, totalDuration) ->
    totalDuration + maintenance.getUsageTime() > maintenance.getCapacity())
    .penalize(HardMediumSoftScore.ONE_HARD,
    (maintenance, totalDuration) -> {
    int exceeded = totalDuration + maintenance.getUsageTime() - maintenance.getCapacity();
    return exceeded * HARD_PENALTY_WEIGHT;
    })
    .asConstraint("硬约束：不能超过维护容量");
- 工作中心维护冲突约束：设备维护期间不可用
  - 硬约束5: 外协工序时间约束 - 上一道工序结束时间必须等于该工序开始时间
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot -> timeslot.getProcedure().getWorkCenter() != null &&
    WORK_CENTER_CODE.equals(timeslot.getProcedure().getWorkCenter().getWorkCenterCode()) &&
    timeslot.getProcedure() != null &&
    timeslot.getProcedure().getProcedureNo() > 1 &&
    timeslot.getStartTime() != null)
    .join(Timeslot.class,
    Joiners.equal(t -> t.getProcedure().getTask().getTaskNo(), t -> t.getProcedure().getTask().getTaskNo()),
    Joiners.equal(t -> t.getProcedure().getIndex() - 1, t -> t.getProcedure().getIndex()))
    .filter((current, previous) -> previous.getEndTime() != null && !previous.getEndTime().equals(current.getStartTime()))
    .penalize(HardMediumSoftScore.ONE_HARD,
    (current, previous) -> HARD_PENALTY_WEIGHT * 10)
    .asConstraint("硬约束：外协工序-上一道工序结束时间必须等于该工序开始时间");
- 固定开始时间约束：某些工序可能有固定的开始时间要求
  - 硬约束6: 外协工序时间约束 - 该工序结束时间必须等于下一道工序开始时间
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot -> timeslot.getProcedure().getWorkCenter() != null &&
    WORK_CENTER_CODE.equals(timeslot.getProcedure().getWorkCenter().getWorkCenterCode()) &&
    timeslot.getProcedure() != null &&
    timeslot.getProcedure().getNextProcedureNo() != null && !
    timeslot.getProcedure().getNextProcedureNo().isEmpty() &&
    timeslot.getEndTime() != null)
    .join(Timeslot.class,
    Joiners.equal(t -> t.getProcedure().getTask().getTaskNo(), t -> t.getProcedure().getTask().getTaskNo()),
    Joiners.filtering((current, next) -> next.getProcedure() != null &&
    current.getProcedure().getNextProcedureNo().contains(next.getProcedure().getProcedureNo())))
    .filter((current, next) -> next.getStartTime() != null && !current.getEndTime().equals(next.getStartTime()))
    .penalize(HardMediumSoftScore.ONE_HARD, (current, next) -> HARD_PENALTY_WEIGHT * 10)
    .asConstraint("硬约束：外协工序-该工序结束时间必须等于下一道工序开始时间");



### 中等约束（尽量满足）  HardMediumSoftScore.ONE_MEDIUM
- 中等约束1: 工序顺序约束
  - 违反条件：后序工序在前序工序完成前开始
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot -> timeslot.getProcedure() != null &&
    timeslot.getEndTime() != null && !
    timeslot.getProcedure().getNextProcedure().isEmpty())
    .join(Timeslot.class,
    Joiners.equal(t -> t.getProcedure().getTask().getTaskNo(), t -> t.getProcedure().getTask().getTaskNo()),
    Joiners.filtering((current, next) -> current.getProcedure().getNextProcedure().stream().anyMatch(p -> p.getId().equals(next.getProcedure().getId()))))
    .filter((current, next) ->
    next.getStartTime() != null && !current.getEndTime().isBefore(next.getStartTime()))
    .penalize(HardMediumSoftScore.ONE_MEDIUM,
    (current, next) -> {
    // 如果后序在前序完成前开始，计算提前的时间
    long minutesEarly = Duration.between(next.getStartTime(), current.getEndTime()).toMinutes();
    return (int) Math.max(0, minutesEarly) * MEDIUM_PENALTY_WEIGHT;
    })
    .asConstraint("中约束：工序必须按顺序执行");
- 中等约束3: 订单日期约束
  - 违反条件：任务开始时间早于实际开始时间
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot ->
    timeslot.getProcedure().getTask() != null
    && timeslot.getProcedure().getTask().getFactStartDate() != null
    && timeslot.getStartTime() != null
    && timeslot.getStartTime().isBefore(timeslot.getProcedure().getTask().getFactStartDate()))
    .penalize(HardMediumSoftScore.ONE_MEDIUM,
    timeslot -> {
    long daysEarly =
    Duration.between(timeslot.getStartTime(), timeslot.getProcedure().getTask().getFactStartDate()).toDays();
    return (int) daysEarly * MEDIUM_PENALTY_WEIGHT;
    })
    .asConstraint("中约束：不能早于实际开始时间");


### 软约束（尽量满足）  HardMediumSoftScore.ONE_SOFT
- 软约束1: 奖励提前完成
  - 优化目标：越早完成越好
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot -> timeslot.getEndTime() != null &&
    timeslot.getProcedure() != null &&
    timeslot.getProcedure().getPlanEndDate() != null)
    .reward(HardMediumSoftScore.ONE_SOFT,
    timeslot -> {
    LocalDateTime planEnd = timeslot.getProcedure().getPlanEndDate().atTime(23, 59);
    LocalDateTime actualEnd = timeslot.getEndTime();
    if (actualEnd.isBefore(planEnd)) {
    long daysEarly = Duration.between(actualEnd, planEnd).toDays();
    return (int) daysEarly * SOFT_REWARD_WEIGHT;
    }
    return 0;
    })
    .asConstraint("软约束：奖励提前完成");
- 软约束2: 奖励准时开始
  - 优化目标：按计划时间开始
  - constraintFactory.forEach(Timeslot.class)
    .filter(timeslot -> timeslot.getStartTime() != null &&
    timeslot.getProcedure().getTask() != null &&
    timeslot.getProcedure().getTask().getPlanStartDate() != null &&
    timeslot.getProcedureIndex() == 1)
    .reward(HardMediumSoftScore.ONE_SOFT,
    timeslot -> {
    LocalDateTime planStart = timeslot.getProcedure().getTask().getPlanStartDate().atStartOfDay();
    LocalDateTime actualStart = timeslot.getStartTime();
    long hoursDiff = Math.abs(Duration.between(planStart, actualStart).toHours());
    if (hoursDiff <= 4) {
    return (int) (SOFT_REWARD_WEIGHT * (5 - hoursDiff));
    }
    return 0;
    })
    .asConstraint("软约束：奖励准时开始");

## 求解器配置说明   SolverConfig
    @Bean
    public SolverConfig solverConfig() {
        SolverConfig solverConfig = new SolverConfig();

        // 设置解决方案和实体类
        solverConfig.withSolutionClass(FactorySchedulingSolution.class)
                .withEntityClasses(Timeslot.class)
                .withConstraintProviderClass(FactorySchedulingConstraintProvider.class);

        // 设置终止条件 - 优化以适应多线程环境
        solverConfig.withTerminationConfig(new TerminationConfig()
                .withSecondsSpentLimit(180L)  // 增加总体时间限制以利用多线程优势
                .withBestScoreLimit("0hard/0medium/10000soft")
                .withUnimprovedSecondsSpentLimit(60L));  // 添加未改进时间限制，避免无效计算

        // 配置阶段 - 使用更简单的配置
        List<PhaseConfig> phaseConfigList = new ArrayList<>();
        
        // 1. 构造启发式阶段
        phaseConfigList.add(new ConstructionHeuristicPhaseConfig());
        
        // 2. 局部搜索阶段 - 简化配置
        phaseConfigList.add(new LocalSearchPhaseConfig());
        
        solverConfig.setPhaseConfigList(phaseConfigList);

        // 设置环境模式 - 在多线程环境中使用REPRODUCIBLE确保结果可重现
        // 注意：在生产环境中可考虑使用FASTEST，但会牺牲结果可重现性
        solverConfig.setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        
        // 多线程配置 - 启用并行移动生成和评估
        // 使用自动设置以充分利用多核CPU
//        solverConfig.setMoveThreadCount(MOVE_THREAD_COUNT_AUTO);

        // 增加移动线程缓冲区大小以减少线程间竞争
        solverConfig.setMoveThreadBufferSize(1024);
        
        // 设置随机数种子以提高多线程环境下的稳定性
        solverConfig.setRandomSeed(42L);

        return solverConfig;
    }


## 任务分配解决方案   分配Timeslot给工序Procedure
        /**
     * 时间槽（工序）列表 - 规划实体集合
     */
    private List<Timeslot> timeslots;


    /**
     * 设备维护计划列表 - 影响工作中心可用性的约束条件和规划变量取值范围
     * <p>在维护期间，对应的工作中心不可用</p>
     */
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

## 启动调度求解Solver。solve() 传入问题ID，用于唯一标识本次调度任务 orderNos 需要参与调度的订单编号列表
    保存最终调度结果到数据库

## 主要问题是
  - 按哪些标准进行排
  - 约束条件怎怎么订
  - 如何方便扩展
  - 如何优化计算时间


