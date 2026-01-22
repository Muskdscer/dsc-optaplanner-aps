package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import com.upec.factoryscheduling.aps.repository.WorkCenterMaintenanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工作中心维护服务
 * <p>
 * 负责管理工厂中工作中心（设备/机器）的维护计划，包括自动生成维护计划、保存、更新、查询和删除维护记录等功能。
 * </p>
 * <p>
 * 该服务确保在调度过程中考虑设备维护时间，避免在维护期间安排生产任务。
 * </p>
 */
@Service // 标记此类为Spring服务组件
public class WorkCenterMaintenanceService {

    /** 设备维护仓库 - 用于访问设备维护数据 */
    private WorkCenterMaintenanceRepository maintenanceRepository;

    /** 工作中心服务 - 提供工作中心相关的业务逻辑 */
    private WorkCenterService workCenterService;

    /**
     * 设置维护仓库
     * 
     * @param maintenanceRepository 设备维护仓库实例
     */
    @Autowired
    public void setMaintenanceRepository(WorkCenterMaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }

    /**
     * 设置工作中心服务
     * 
     * @param workCenterService 工作中心服务实例
     */
    @Autowired
    public void setMachineService(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }

    /**
     * 保存单个设备维护记录
     * <p>
     * 将单个设备维护记录保存到数据库中。
     * </p>
     * 
     * @param maintenance 设备维护记录对象
     * @return 保存后的设备维护记录
     */
    @Transactional("mysqlTransactionManager") // 声明事务
    public WorkCenterMaintenance save(WorkCenterMaintenance maintenance) {
        return maintenanceRepository.save(maintenance);
    }

    /**
     * 保存多个设备维护记录
     * <p>
     * 批量保存设备维护记录列表到数据库中。
     * </p>
     * 
     * @param maintenances 设备维护记录列表
     * @return 保存后的设备维护记录列表
     */
    @Transactional("mysqlTransactionManager") // 声明事务
    public List<WorkCenterMaintenance> saveAll(List<WorkCenterMaintenance> maintenances) {
        return maintenanceRepository.saveAll(maintenances);
    }

    /**
     * 为单个设备自动创建维护计划
     * <p>
     * 为指定的工作中心（设备）自动生成未来30天的维护计划，每天上午9点开始，持续8小时（480分钟）。
     * </p>
     * 
     * @param machine 工作中心（设备）对象
     * @return 生成的维护计划列表
     */
    @Transactional("mysqlTransactionManager") // 声明事务
    public List<WorkCenterMaintenance> autoCreateMaintenance(WorkCenter machine) {
        LocalDate now = LocalDate.now();
        List<WorkCenterMaintenance> maintenances = new ArrayList<>();
        // 为未来30天每天创建维护计划
        for (int i = 1; i <= 30; i++) {
            // 创建维护计划对象，持续时间为480分钟（8小时）
            WorkCenterMaintenance maintenance = new WorkCenterMaintenance(machine, now.plusDays(i), 480, null);
            maintenance.setStartTime(LocalTime.of(9, 0)); // 上午9点开始
            maintenance.setEndTime(maintenance.getStartTime().plusMinutes(maintenance.getCapacity())); // 计算结束时间
            maintenances.add(maintenance);
        }

        return saveAll(maintenances);
    }

    /**
     * 为所有设备自动创建维护计划
     * <p>
     * 为系统中的所有工作中心（设备）自动生成未来30天的维护计划。
     * </p>
     * 
     * @return 所有设备生成的维护计划列表
     */
    public List<WorkCenterMaintenance> auto() {
        // 获取所有工作中心（设备）
        List<WorkCenter> machines = workCenterService.getAllMachines();
        List<WorkCenterMaintenance> maintenances = new ArrayList<>();
        // 为每个设备生成维护计划并添加到结果列表
        for (WorkCenter machine : machines) {
            maintenances.addAll(autoCreateMaintenance(machine));
        }
        return maintenances;
    }

    /**
     * 获取所有维护计划
     * <p>
     * 查询数据库中所有的设备维护计划记录。
     * </p>
     * 
     * @return 所有维护计划列表
     */
    public List<WorkCenterMaintenance> getAllMaintenances() {
        return maintenanceRepository.findAll();
    }

    /**
     * 批量更新维护计划
     * <p>
     * 根据传入的维护计划列表更新数据库中的记录，包括状态、开始时间、结束时间和容量等信息。
     * </p>
     * 
     * @param maintenances 待更新的维护计划列表
     * @return 更新后的维护计划列表
     * @throws IllegalArgumentException 如果开始时间晚于结束时间
     */
    @Transactional("mysqlTransactionManager")
    public List<WorkCenterMaintenance> updateAll(List<WorkCenterMaintenance> maintenances) {
        List<WorkCenterMaintenance> list = maintenances.stream().map(maintenance -> {
            // 验证时间有效性
            if (maintenance.getStartTime().isAfter(maintenance.getEndTime())) {
                throw new IllegalArgumentException("开始时间不能晚于结束时间");
            }
            // 查找数据库中对应的记录
            WorkCenterMaintenance workCenterMaintenance =
                    maintenanceRepository.findById(maintenance.getId()).orElse(null);
            if (workCenterMaintenance == null) {
                return null; // 记录不存在，跳过
            }
            // 更新字段
            if (maintenance.getStatus() != null) {
                workCenterMaintenance.setStatus(maintenance.getStatus());
            }
            if (maintenance.getStartTime() != null) {
                workCenterMaintenance.setStartTime(maintenance.getStartTime());
            }
            if (maintenance.getEndTime() != null) {
                workCenterMaintenance.setEndTime(maintenance.getEndTime());
            }
            // 计算容量（持续时间）
            assert maintenance.getEndTime() != null;
            long durationMinutes = java.time.Duration.between(maintenance.getStartTime(), maintenance.getEndTime()).toMinutes();
            workCenterMaintenance.setCapacity((int) durationMinutes);

            return workCenterMaintenance;
        }).filter(Objects::nonNull) // 过滤掉null值（不存在的记录）
                .collect(Collectors.toList());

        return maintenanceRepository.saveAll(list);
    }


    /**
     * 保存或更新所有维护计划
     * <p>
     * 根据日期检查维护计划是否已存在，如果存在则更新，不存在则新增。
     * </p>
     * <p>
     * 注意：此方法查找的是特定日期且工作中心为空的维护计划，可能是用于全局维护或特殊日期维护。
     * </p>
     * 
     * @param maintenances 待保存或更新的维护计划列表
     */
    @Transactional("mysqlTransactionManager")
    public void saveAllMaintenance(List<WorkCenterMaintenance> maintenances) {
        if (!CollectionUtils.isEmpty(maintenances)) {
            for (WorkCenterMaintenance maintenance : maintenances) {
                // 根据日期和工作中心为空的条件查找维护计划
                WorkCenterMaintenance workCenterMaintenance =
                        maintenanceRepository.findFirstByDateAndWorkCenterIsNull(maintenance.getDate());

                if (workCenterMaintenance == null) {
                    // 记录不存在，直接保存
                    maintenanceRepository.save(maintenance);
                } else {
                    // 记录存在，更新字段
                    workCenterMaintenance.setEndTime(maintenance.getEndTime());
                    workCenterMaintenance.setStartTime(maintenance.getStartTime());
                    workCenterMaintenance.setStatus(maintenance.getStatus());
                    workCenterMaintenance.setCapacity(maintenance.getCapacity());
                    workCenterMaintenance.setDescription(maintenance.getDescription());
                    maintenanceRepository.save(workCenterMaintenance);
                }
            }
        }
    }


    /**
     * 创建设备维护计划
     * <p>
     * 批量创建设备维护计划并保存到数据库。
     * </p>
     * 
     * @param maintenances 待创建的维护计划列表
     * @return 创建后的维护计划列表
     */
    @Transactional("mysqlTransactionManager")
    public List<WorkCenterMaintenance> createMachineMaintenance(List<WorkCenterMaintenance> maintenances) {
        return maintenanceRepository.saveAll(maintenances);
    }


    /**
     * 根据设备和日期查找维护计划
     * <p>
     * 查找指定设备在特定日期的维护计划记录。
     * </p>
     * 
     * @param machine 工作中心（设备）
     * @param date 查询日期
     * @return 对应的维护计划记录，如果不存在则返回null
     */
    public WorkCenterMaintenance findFirstByMachineAndDate(WorkCenter machine, LocalDate date) {
        return maintenanceRepository.findFirstByWorkCenterAndDate(machine, date);
    }

    /**
     * 查找多个设备在日期范围内的维护计划
     * <p>
     * 查询指定多个设备在指定日期范围内的所有维护计划记录。
     * </p>
     * 
     * @param workCenters 工作中心（设备）列表
     * @param start 开始日期
     * @param end 结束日期
     * @return 符合条件的维护计划列表
     */
    public List<WorkCenterMaintenance> findAllByMachineInAndDateBetween(List<WorkCenter> workCenters, LocalDate start, LocalDate end) {
        return maintenanceRepository.findAllByWorkCenterInAndDateBetween(workCenters, start, end);
    }

    /**
     * 删除所有维护计划
     * <p>
     * 删除数据库中所有的设备维护计划记录，通常用于测试或重置环境。
     * </p>
     */
    @Transactional("mysqlTransactionManager")
    public void deleteAll() {
        maintenanceRepository.deleteAll();
    }
}
