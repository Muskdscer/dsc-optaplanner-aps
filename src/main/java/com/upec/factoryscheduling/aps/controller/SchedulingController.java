package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.resquest.ProcedureRequest;
import com.upec.factoryscheduling.aps.service.SchedulingService;
import com.upec.factoryscheduling.aps.service.TimeslotService;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 调度控制器
 * <p>提供工厂调度相关的REST API接口，管理调度过程的全生命周期，包括启动、停止、查询状态和获取结果等功能。</p>
 * <p>该控制器作为前端与后端调度服务之间的桥梁，接收调度请求并将处理结果返回给客户端。</p>
 */
@RestController  // 标记此类为REST控制器
@RequestMapping("/api/scheduling")  // API基础路径
@Slf4j  // Lombok注解，提供日志记录功能
@CrossOrigin  // 允许跨域请求访问
public class SchedulingController {

    /** 调度服务 - 提供工厂调度核心业务逻辑 */
    private final SchedulingService schedulingService;

    /**
     * 构造函数
     * @param schedulingService 调度服务，用于处理核心调度逻辑
     */
    @Autowired
    public SchedulingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    /** 时间槽服务 - 提供时间槽相关的业务逻辑 */
    private TimeslotService timeslotService;

    /**
     * 设置时间槽服务
     * @param timeslotService 时间槽服务，用于处理时间槽相关操作
     */
    @Autowired
    public void setTimeslotService(TimeslotService timeslotService) {
        this.timeslotService = timeslotService;
    }

    /**
     * 启动调度求解
     * <p>根据指定的问题ID和订单编号列表开始调度优化过程，触发OptaPlanner求解器进行排程计算。</p>
     * 
     * @param problemId 问题ID，用于唯一标识本次调度任务
     * @param orderNos 需要参与调度的订单编号列表
     * @return 操作结果，包含成功消息
     */
    @PostMapping("/solve/{problemId}")
    public ApiResponse<String> startScheduling(@PathVariable Long problemId, @RequestBody List<String> orderNos) {
        schedulingService.startScheduling(problemId, orderNos);
        return ApiResponse.success("Scheduling started for problem " + problemId);
    }

    /**
     * 停止调度求解
     * <p>停止指定问题ID的调度求解过程，释放计算资源。</p>
     * 
     * @param problemId 问题ID，指定要停止的调度任务
     * @return HTTP响应，包含操作结果消息
     */
    @PostMapping("/stop/{problemId}")
    public ApiResponse<String> stopScheduling(@PathVariable Long problemId) {
        schedulingService.stopScheduling(problemId);
        return ApiResponse.success("Scheduling stopped for problem " + problemId);
    }

    /**
     * 获取最佳解决方案
     * <p>获取指定问题ID的当前最佳调度解决方案，包含所有已优化的时间槽安排。</p>
     * 
     * @param problemId 问题ID，指定要获取解决方案的调度任务
     * @return 包含最佳解决方案的HTTP响应
     */
    @GetMapping("/solution/{problemId}")
    public ApiResponse<FactorySchedulingSolution> getBestSolution(@PathVariable Long problemId) {
        FactorySchedulingSolution solution = schedulingService.getBestSolution(problemId);
        // 注意：此处有一段无实际作用的循环代码，在实际优化中应移除
        for (Timeslot timeslot : solution.getTimeslots()) {
            int i = 1;
        }
        return ApiResponse.success(solution);
    }

    /**
     * 获取解决方案评分
     * <p>获取指定问题ID的当前最佳解决方案的评分，包括硬约束和软约束的评估结果。</p>
     * 
     * @param problemId 问题ID，指定要获取评分的调度任务
     * @return 包含硬软评分的HTTP响应
     */
    @GetMapping("/score/{problemId}")
    public ApiResponse<HardMediumSoftScore> getScore(@PathVariable Long problemId) {
        HardMediumSoftScore hardSoftScore = schedulingService.getScore(problemId);
        return ApiResponse.success(hardSoftScore);
    }

    /**
     * 获取求解状态
     * <p>获取指定问题ID的当前调度任务的求解状态，如正在求解、已完成、未开始等。</p>
     * 
     * @param problemId 问题ID，指定要查询状态的调度任务
     * @return 包含求解状态名称的HTTP响应
     */
    @GetMapping("/status/{problemId}")
    public ApiResponse<String> getStatus(@PathVariable Long problemId) {
        SolverStatus isSolving = schedulingService.isSolving(problemId);
        return ApiResponse.success(isSolving.name());
    }

    /**
     * 检查解决方案是否可行
     * <p>判断指定问题ID的当前最佳解决方案是否满足所有硬约束条件，即是否为可行解。</p>
     * 
     * @param problemId 问题ID，指定要检查可行性的调度任务
     * @return 包含可行性布尔值的HTTP响应（true表示可行）
     */
    @GetMapping("/feasible/{problemId}")
    public ApiResponse<Boolean> isSolutionFeasible(@PathVariable Long problemId) {
        boolean isFeasible = schedulingService.isSolutionFeasible(problemId);
        return ApiResponse.success(isFeasible);
    }

    /**
     * 更新调度问题
     * <p>使用新的解决方案数据更新指定问题ID的调度任务，通常用于手动调整后的重新优化。</p>
     * 
     * @param problemId 问题ID，指定要更新的调度任务
     * @param updatedSolution 更新后的解决方案数据
     * @return 包含操作结果消息的HTTP响应
     */
    @PutMapping("/update/{problemId}")
    public ApiResponse<String> updateProblem(@PathVariable Long problemId, @RequestBody FactorySchedulingSolution updatedSolution) {
        schedulingService.updateProblem(problemId, updatedSolution);
        return ApiResponse.success("Problem updated for " + problemId);
    }
    


    /**
     * 获取解决方案详细解释
     * <p>获取指定问题ID的当前最佳解决方案的详细评分解释，包括各约束条件的贡献和违反情况。</p>
     * 
     * @param problemId 问题ID，指定要获取解释的调度任务
     * @return 包含评分解释的HTTP响应，可用于分析调度结果的质量
     */
    @GetMapping("/explain/{problemId}")
    public ApiResponse<ScoreExplanation<FactorySchedulingSolution, HardMediumSoftScore>> getExplanation(@PathVariable Long problemId) {
        return ApiResponse.success(schedulingService.explainSolution(problemId));
    }


    /**
     * 更新时间槽
     * <p>根据请求数据更新指定的时间槽信息，通常用于手动调整特定工序的安排。</p>
     * 
     * @param request 包含更新信息的工序请求
     * @return 包含更新后时间槽的HTTP响应
     */
    @PostMapping("/update")
    public ApiResponse<Timeslot> update(@RequestBody ProcedureRequest request) {
        return ApiResponse.success(timeslotService.updateTimeslot(request));
    }

    /**
     * 删除所有调度数据
     * <p>删除系统中的所有调度相关数据，通常用于测试或重置环境。</p>
     * 
     * @return 包含操作结果消息的HTTP响应
     */
    @GetMapping("/deleteAll")
    public ApiResponse<String> delete() {
        schedulingService.delete();
        return ApiResponse.success("success");
    }

    /**
     * 验证解决方案
     * <p>验证传入的解决方案是否满足约束条件，并返回验证后的结果。</p>
     * 
     * @param solution 要验证的解决方案
     * @return 包含验证后解决方案的HTTP响应
     */
    @PostMapping("/validation")
    public ApiResponse<FactorySchedulingSolution> validation(@RequestBody FactorySchedulingSolution solution) {
        return ApiResponse.success(schedulingService.validation(solution));
    }

    /**
     * 接收时间槽数据
     * <p>接收并处理传入的时间槽列表数据。目前返回空列表，可能需要根据业务需求完善实现。</p>
     * 
     * @param timeslots 要处理的时间槽列表
     * @return 包含处理结果的HTTP响应（目前返回空列表）
     */
    @PostMapping("/receiveTimeslot")
    public ApiResponse<List<Timeslot>> receiveTimeslot(@RequestBody List<Timeslot> timeslots) {
        // 注意：当前实现返回空列表，可能需要根据实际业务需求完善
        return ApiResponse.success(new ArrayList<>());
    }
}
