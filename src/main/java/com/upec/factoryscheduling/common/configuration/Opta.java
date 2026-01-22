package com.upec.factoryscheduling.common.configuration;//package com.upec.factoryscheduling.common.configuration;
//
//import com.upec.factoryscheduling.aps.entity.Timeslot;
//import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
//import com.upec.factoryscheduling.aps.solver.FactorySchedulingConstraintProvider;
//import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
//import org.optaplanner.core.api.solver.SolutionManager;
//import org.optaplanner.core.api.solver.SolverManager;
//import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
//import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
//import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
//import org.optaplanner.core.config.localsearch.LocalSearchType;
//import org.optaplanner.core.config.phase.PhaseConfig;
//import org.optaplanner.core.config.solver.EnvironmentMode;
//import org.optaplanner.core.config.solver.SolverConfig;
//import org.optaplanner.core.config.solver.SolverManagerConfig;
//import org.optaplanner.core.config.solver.termination.TerminationConfig;
//import org.springframework.context.annotation.Bean;
//
//import java.util.List;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
//import static org.optaplanner.core.config.solver.SolverConfig.MOVE_THREAD_COUNT_AUTO;
//
//
//public class Opta{
//
//    @Bean
//    public SolverConfig solverConfig() {
//        SolverConfig solverConfig = new SolverConfig();
//
//        // 设置解决方案和实体类
//        solverConfig.withSolutionClass(FactorySchedulingSolution.class)
//                .withEntityClasses(Timeslot.class)
//                .withConstraintProviderClass(FactorySchedulingConstraintProvider.class);
//
//        // 设置基本求解器配置
//        // 注意：当前OptaPlanner版本可能不支持incrementalSolvingEnabled，使用默认配置
//
//        // 设置终止条件
//        solverConfig.withTerminationConfig(new TerminationConfig()
//                .withSecondsSpentLimit(300L)
//                .withBestScoreLimit("0hard/0soft"));
//
//        // 设置阶段配置，确保支持增量更新
//        List<PhaseConfig> phaseConfigList = List.of(
//                // 构造启发式阶段 - 使用增量模式
//                new ConstructionHeuristicPhaseConfig()
//                        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT_DECREASING)
//                        .withTerminationConfig(new TerminationConfig()
//                                .withStepCountLimit(1000)),
//
//                // 局部搜索阶段 - 使用增量模式和适当的启发式算法
//                new LocalSearchPhaseConfig()
//                        .withLocalSearchType(LocalSearchType.LATE_ACCEPTANCE)
//                        // 使用基本的Local Search配置
//                        // 注意：当前OptaPlanner版本可能不支持withLateAcceptanceSize方法
//                        .withTerminationConfig(new TerminationConfig()
//                                .withUnimprovedSecondsSpentLimit(120L))
//        );
//
//        solverConfig.setPhaseConfigList(phaseConfigList);
//
//        // 设置环境模式 - 在生产环境中使用REPRODUCIBLE以确保结果可重现
//        solverConfig.setEnvironmentMode(EnvironmentMode.REPRODUCIBLE);
//
//        // 设置随机数种子以提高多线程环境下的稳定性
//        solverConfig.setRandomSeed(42L);
//
//        // 并行求解配置
//        // 1. 移动线程数 - 使用自动设置以充分利用多核CPU
//        solverConfig.setMoveThreadCount(MOVE_THREAD_COUNT_AUTO);
//
//        // 2. 配置并行搜索的冲突解决策略
//        solverConfig.setMoveThreadBufferSize(1024); // 增加缓冲区以减少线程间竞争
//
//        // 3. 优化分数计算性能
//        // 移除了不兼容的ConstraintStreamImplType设置，使用默认实现以确保兼容性
//
//        return solverConfig;
//    }
//
//    @Bean
//    public SolverManager<FactorySchedulingSolution, Long> solverManager(SolverConfig solverConfig) {
//        // 配置SolverManager以支持多线程增量求解
//        SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
//
//        // 获取处理器核心数
//        int availableProcessors = Runtime.getRuntime().availableProcessors();
//
//        // 设置并行求解器的数量，使用处理器核心数的一半以平衡性能和资源消耗
//        solverManagerConfig.setParallelSolverCount(String.valueOf(availableProcessors / 2));
//        // 配置自定义线程池以优化多线程增量求解性能
//        // 创建具有适当参数的线程池，确保线程重用和有序关闭
//        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
//                availableProcessors,  // 线程池大小
//                r -> {
//                    Thread thread = new Thread(r, "optaplanner-solver-");
//                    thread.setDaemon(true);  // 设置为守护线程，允许JVM正常关闭
//                    thread.setPriority(Thread.NORM_PRIORITY);
//                    return thread;
//                }
//        );
//
//        // 配置线程池参数以优化性能和资源利用
//        threadPoolExecutor.setKeepAliveTime(60, TimeUnit.SECONDS);
//        threadPoolExecutor.allowCoreThreadTimeOut(true);
//        // 使用配置创建SolverManager
//        return SolverManager.create(
//                solverConfig,
//                solverManagerConfig
//        );
//    }
//
//    @Bean
//    public SolutionManager<FactorySchedulingSolution, HardSoftScore> solutionManager(SolverManager<FactorySchedulingSolution, Long> solverManager) {
//        // 使用SolverManager创建SolutionManager
//        return SolutionManager.create(solverManager);
//    }
//}
