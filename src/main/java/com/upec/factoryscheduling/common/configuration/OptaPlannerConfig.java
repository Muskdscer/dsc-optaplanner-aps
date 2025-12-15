package com.upec.factoryscheduling.common.configuration;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.aps.solver.FactorySchedulingConstraintProvider;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static org.optaplanner.core.config.solver.SolverConfig.MOVE_THREAD_COUNT_AUTO;

@Configuration
public class OptaPlannerConfig {

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

    @Bean
    public SolverManager<FactorySchedulingSolution, Long> solverManager(SolverConfig solverConfig) {
        // 创建SolverManager，使用版本兼容的方式
        // 注意：当前OptaPlanner版本可能不支持SolverManagerConfig，使用基本创建方式
        return SolverManager.create(solverConfig);
    }

    @Bean
    public SolutionManager<FactorySchedulingSolution, HardMediumSoftScore> solutionManager(SolverManager<FactorySchedulingSolution,
            Long> solverManager) {
        // 使用SolverManager创建SolutionManager
        return SolutionManager.create(solverManager);
    }
}
