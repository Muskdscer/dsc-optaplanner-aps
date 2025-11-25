package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class TimeslotVariableListener implements VariableListener<FactorySchedulingSolution, Timeslot> {
    
    /**
     * 工序时间槽缓存 - 优化性能，避免重复遍历
     * Key: 工序ID, Value: 该工序的所有时间槽列表
     */
    private Map<String, List<Timeslot>> procedureTimeslotCache = new HashMap<>();

    @Override
    public void beforeVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 不需要在变量改变前进行特殊处理，但需要确保scoreDirector和timeslot不为空
        if (scoreDirector == null || timeslot == null) {
            log.warn("Null parameters in beforeVariableChanged: scoreDirector={}, timeslot={}", 
                     scoreDirector != null, timeslot != null);
        }
    }

    @Override
    public void afterVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 确保参数不为空
        if (scoreDirector == null || timeslot == null) {
            log.warn("Null parameters in afterVariableChanged: scoreDirector={}, timeslot={}", 
                     scoreDirector != null, timeslot != null);
            return;
        }
        
        // 统一处理时间槽更新逻辑
        updateTimeslotTiming(scoreDirector, timeslot);
        updateProcedureTimes(scoreDirector, timeslot);
    }

    /**
     * 统一处理时间槽时间更新逻辑 - 优化版本
     * 合并手动和自动时间槽的处理逻辑，减少代码重复
     */
    private void updateTimeslotTiming(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        if (scoreDirector == null || timeslot == null) {
            return;
        }
        
        // 只有当startTime不为null时才更新endTime
        if (timeslot.getStartTime() != null) {
            updateEndTime(scoreDirector, timeslot);
        } else {
            // 如果startTime为null，重置endTime
            scoreDirector.beforeVariableChanged(timeslot, "endTime");
            timeslot.setEndTime(null);
            scoreDirector.afterVariableChanged(timeslot, "endTime");
        }
    }
    
    private void updateEndTime(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        if (scoreDirector == null || timeslot == null) {
            return;
        }
        
        // 计算并更新endTime字段 - 优化计算逻辑
        if (timeslot.getDuration() > 0) {
            scoreDirector.beforeVariableChanged(timeslot, "endTime");
            // 使用更精确的分钟计算
            long durationMinutes = (long) (timeslot.getDuration() * 60);
            timeslot.setEndTime(timeslot.getStartTime().plusMinutes(durationMinutes));
            scoreDirector.afterVariableChanged(timeslot, "endTime");
        } else {
            // 如果duration无效，重置endTime
            scoreDirector.beforeVariableChanged(timeslot, "endTime");
            timeslot.setEndTime(null);
            scoreDirector.afterVariableChanged(timeslot, "endTime");
        }
    }

    /**
     * 优化版本的工序时间更新 - 使用缓存机制提高性能
     * 只在必要时更新工序的开始和结束时间
     */
    private void updateProcedureTimes(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        if (timeslot == null || timeslot.getProcedure() == null || scoreDirector.getWorkingSolution() == null) {
            return;
        }
        
        String procedureId = timeslot.getProcedure().getId();
        
        // 使用缓存或重新构建工序时间槽列表
        List<Timeslot> procedureTimeslots = getProcedureTimeslots(scoreDirector, procedureId);
        
        if (procedureTimeslots.isEmpty()) {
            // 如果没有有效时间槽，重置工序时间
            Procedure procedure = timeslot.getProcedure();
            procedure.setStartTime(null);
            procedure.setEndTime(null);
            return;
        }
        
        // 使用单次遍历找到最早开始和最晚结束时间
        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;
        boolean hasValidTimes = false;
        
        for (Timeslot t : procedureTimeslots) {
            if (t.getStartTime() != null) {
                hasValidTimes = true;
                if (earliestStart == null || t.getStartTime().isBefore(earliestStart)) {
                    earliestStart = t.getStartTime();
                }
            }
            if (t.getEndTime() != null) {
                hasValidTimes = true;
                if (latestEnd == null || t.getEndTime().isAfter(latestEnd)) {
                    latestEnd = t.getEndTime();
                }
            }
        }
        
        // 只有当找到有效的时间值时才更新工序时间
        if (hasValidTimes) {
            Procedure procedure = timeslot.getProcedure();
            
            // 批量更新，减少触发器调用
            boolean needUpdate = false;
            if (earliestStart != null && !Objects.equals(procedure.getStartTime(), earliestStart)) {
                procedure.setStartTime(earliestStart);
                needUpdate = true;
            }
            
            if (latestEnd != null && !Objects.equals(procedure.getEndTime(), latestEnd)) {
                procedure.setEndTime(latestEnd);
                needUpdate = true;
            }
            
            if (needUpdate) {
                log.debug("Updated procedure {} times: start={}, end={}", 
                         procedureId, earliestStart, latestEnd);
            }
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 确保参数不为空
        if (scoreDirector == null || timeslot == null) {
            log.warn("Null parameters in beforeEntityAdded: scoreDirector={}, timeslot={}", 
                     scoreDirector != null, timeslot != null);
        }
    }

    @Override
    public void afterEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 确保参数不为空
        if (scoreDirector == null || timeslot == null) {
            log.warn("Null parameters in afterEntityAdded: scoreDirector={}, timeslot={}", 
                     scoreDirector != null, timeslot != null);
            return;
        }
        
        // 更新时间槽缓存
        updateProcedureCache(timeslot, true);
        
        // 统一处理时间槽更新逻辑
        updateTimeslotTiming(scoreDirector, timeslot);
        updateProcedureTimes(scoreDirector, timeslot);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 确保参数不为空
        if (scoreDirector == null || timeslot == null) {
            log.warn("Null parameters in beforeEntityRemoved: scoreDirector={}, timeslot={}", 
                     scoreDirector != null, timeslot != null);
        }
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 更新时间槽缓存
        updateProcedureCache(timeslot, false);
        
        // 如果删除了工序的最后一个分片，更新工序时间
        if (timeslot.getProcedure() != null) {
            String procedureId = timeslot.getProcedure().getId();
            List<Timeslot> remainingTimeslots = getProcedureTimeslots(scoreDirector, procedureId);
            
            // 从剩余时间槽中排除当前被删除的时间槽
            remainingTimeslots.removeIf(t -> t.getId().equals(timeslot.getId()));
            
            if (remainingTimeslots.isEmpty()) {
                // 如果没有剩余时间槽，重置工序的开始和结束时间
                Procedure procedure = timeslot.getProcedure();
                procedure.setStartTime(null);
                procedure.setEndTime(null);
                log.debug("Cleared procedure {} times - no remaining timeslots", procedureId);
            } else {
                // 使用剩余时间槽中的任意一个来更新工序时间（选择第一个有效的）
                Timeslot validTimeslot = remainingTimeslots.stream()
                    .filter(t -> t != null && t.getStartTime() != null)
                    .findFirst()
                    .orElse(null);
                    
                if (validTimeslot != null) {
                    updateProcedureTimes(scoreDirector, validTimeslot);
                }
            }
        }
    }
    
    /**
     * 获取指定工序的时间槽列表 - 使用缓存优化性能
     * 如果缓存中没有，则重新构建并缓存
     */
    private List<Timeslot> getProcedureTimeslots(ScoreDirector<FactorySchedulingSolution> scoreDirector, String procedureId) {
        if (procedureId == null || scoreDirector.getWorkingSolution() == null) {
            return new ArrayList<>();
        }
        
        // 检查缓存
        if (procedureTimeslotCache.containsKey(procedureId)) {
            return new ArrayList<>(procedureTimeslotCache.get(procedureId));
        }
        
        // 重新构建工序时间槽列表
        List<Timeslot> procedureTimeslots = new ArrayList<>();
        for (Timeslot t : scoreDirector.getWorkingSolution().getTimeslots()) {
            if (t != null && t.getProcedure() != null && 
                procedureId.equals(t.getProcedure().getId())) {
                procedureTimeslots.add(t);
            }
        }
        
        // 缓存结果
        procedureTimeslotCache.put(procedureId, new ArrayList<>(procedureTimeslots));
        return procedureTimeslots;
    }
    
    /**
     * 更新工序时间槽缓存
     * @param timeslot 要更新的时间槽
     * @param isAdd 是否为添加操作
     */
    private void updateProcedureCache(Timeslot timeslot, boolean isAdd) {
        if (timeslot == null || timeslot.getProcedure() == null) {
            return;
        }
        
        String procedureId = timeslot.getProcedure().getId();
        
        if (isAdd) {
            // 添加时间槽到缓存
            procedureTimeslotCache.computeIfAbsent(procedureId, k -> new ArrayList<>()).add(timeslot);
        } else {
            // 从缓存中移除时间槽
            if (procedureTimeslotCache.containsKey(procedureId)) {
                procedureTimeslotCache.get(procedureId).remove(timeslot);
                // 如果该工序没有剩余时间槽，移除缓存条目
                if (procedureTimeslotCache.get(procedureId).isEmpty()) {
                    procedureTimeslotCache.remove(procedureId);
                }
            }
        }
    }
}
