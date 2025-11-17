package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.api.domain.variable.VariableListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TimeslotVariableListener implements VariableListener<FactorySchedulingSolution, Timeslot> {

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
        
        // 对于手动设置的时间槽，直接设置dateTime为startTime
        if (timeslot.isManual() && timeslot.getStartTime() != null) {
            scoreDirector.beforeVariableChanged(timeslot, "dateTime");
            timeslot.setStartTime(timeslot.getStartTime());
            scoreDirector.afterVariableChanged(timeslot, "dateTime");
            
            // 如果手动设置了startTime，也需要更新endTime
            if (timeslot.getDuration() > 0) {
                scoreDirector.beforeVariableChanged(timeslot, "endTime");
                timeslot.setEndTime(timeslot.getStartTime().plusMinutes((long) (timeslot.getDuration() * 60)));
                scoreDirector.afterVariableChanged(timeslot, "endTime");
            }
        } else {
            // 当startTime或workCenter改变时更新相关变量
            updateDateTime(scoreDirector, timeslot);
            updateEndTime(scoreDirector, timeslot);
        }
        updateProcedureTimes(scoreDirector, timeslot);
    }

    private void updateDateTime(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 确保参数不为空
        if (scoreDirector == null || timeslot == null) {
            return;
        }
        
        // 更新dateTime字段，与startTime保持一致
        scoreDirector.beforeVariableChanged(timeslot, "dateTime");
        timeslot.setStartTime(timeslot.getStartTime());
        scoreDirector.afterVariableChanged(timeslot, "dateTime");
    }
    
    private void updateEndTime(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 确保参数不为空
        if (scoreDirector == null || timeslot == null) {
            return;
        }
        
        // 计算并更新endTime字段
        if (timeslot.getStartTime() != null && timeslot.getDuration() > 0) {
            scoreDirector.beforeVariableChanged(timeslot, "endTime");
            timeslot.setEndTime(timeslot.getStartTime().plusMinutes((long) (timeslot.getDuration() * 60)));
            scoreDirector.afterVariableChanged(timeslot, "endTime");
        } else if (timeslot.getStartTime() == null) {
            // 如果startTime为null，重置endTime
            scoreDirector.beforeVariableChanged(timeslot, "endTime");
            timeslot.setEndTime(null);
            scoreDirector.afterVariableChanged(timeslot, "endTime");
        }
    }

    private void updateProcedureTimes(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        if (timeslot == null || timeslot.getProcedure() == null || scoreDirector.getWorkingSolution() == null) {
            return;
        }
        
        String procedureId = timeslot.getProcedure().getId();
        List<Timeslot> procedureTimeslots = new ArrayList<>();
        
        // 直接遍历并过滤，减少流操作的开销
        for (Timeslot t : scoreDirector.getWorkingSolution().getTimeslots()) {
            if (t != null && t.getProcedure() != null && 
                t.getProcedure().getId().equals(procedureId) && 
                t.getStartTime() != null) {
                procedureTimeslots.add(t);
            }
        }
        
        if (procedureTimeslots.isEmpty()) {
            return;
        }
        
        // 手动寻找最小和最大值，避免多次流操作
        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;
        
        for (Timeslot t : procedureTimeslots) {
            if (t.getStartTime() != null) {
                if (earliestStart == null || t.getStartTime().isBefore(earliestStart)) {
                    earliestStart = t.getStartTime();
                }
            }
            if (t.getEndTime() != null) {
                if (latestEnd == null || t.getEndTime().isAfter(latestEnd)) {
                    latestEnd = t.getEndTime();
                }
            }
        }
        
        // 只有当找到有效的时间值时才更新
        if (earliestStart != null || latestEnd != null) {
            Procedure procedure = timeslot.getProcedure();
            
            if (earliestStart != null) {
                procedure.setStartTime(earliestStart);
            }
            
            if (latestEnd != null) {
                procedure.setEndTime(latestEnd);
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
        
        // 对于手动设置的时间槽，直接设置dateTime为startTime
        if (timeslot.isManual() && timeslot.getStartTime() != null) {
            scoreDirector.beforeVariableChanged(timeslot, "dateTime");
            timeslot.setStartTime(timeslot.getStartTime());
            scoreDirector.afterVariableChanged(timeslot, "dateTime");
        } else {
            updateDateTime(scoreDirector, timeslot);
            updateEndTime(scoreDirector, timeslot);
        }
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
        // 如果删除了工序的最后一个分片，更新工序时间
        if (timeslot.getProcedure() != null) {
            String procedureId = timeslot.getProcedure().getId();
            String timeslotId = timeslot.getId();
            List<Timeslot> remainingTimeslots = new ArrayList<>();
            
            // 使用直接遍历代替流操作，提高性能
            for (Timeslot t : scoreDirector.getWorkingSolution().getTimeslots()) {
                if (t != null && t.getProcedure() != null && 
                    t.getProcedure().getId().equals(procedureId) &&
                    !t.getId().equals(timeslotId)) {
                    remainingTimeslots.add(t);
                }
            }
            
            if (CollectionUtils.isEmpty(remainingTimeslots)) {
                // 如果没有剩余时间槽，重置工序的开始和结束时间
                Procedure procedure = timeslot.getProcedure();
                procedure.setStartTime(null);
                procedure.setEndTime(null);
            } else {
                // 找到一个非空的剩余时间槽来更新工序时间
                for (Timeslot t : remainingTimeslots) {
                    if (t != null) {
                        updateProcedureTimes(scoreDirector, t);
                        break;
                    }
                }
            }
        }
    }
}
