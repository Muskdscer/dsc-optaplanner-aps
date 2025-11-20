package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.common.utils.RandomFun;
import com.upec.factoryscheduling.mes.entity.ApsWorkCenterMaintenance;
import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import com.upec.factoryscheduling.mes.repository.ApsWorkCenterMaintenanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApsWorkCenterMaintenanceService {

    private ApsWorkCenterMaintenanceRepository repository;
    private MesBaseWorkCenterService mesBaseWorkCenterService;

    @Autowired
    public void setRepository(ApsWorkCenterMaintenanceRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setMesBaseWorkCenterService(MesBaseWorkCenterService mesBaseWorkCenterService) {
        this.mesBaseWorkCenterService = mesBaseWorkCenterService;
    }

    @Transactional("mysqlTransactionManager")
    public void createWorkCenterMaintenance(List<MesBaseWorkCenter> mesBaseWorkCenters) {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        for (MesBaseWorkCenter baseWorkCenter : mesBaseWorkCenters) {
            List<ApsWorkCenterMaintenance> workCenterMaintenances = new ArrayList<>();
            for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                ApsWorkCenterMaintenance workCenterMaintenance = new ApsWorkCenterMaintenance();
                workCenterMaintenance.setId(RandomFun.getInstance().getRandom());
                workCenterMaintenance.setStatus("Active");
                workCenterMaintenance.setWorkCenterCode(baseWorkCenter.getWorkCenterCode());
                workCenterMaintenance.setLocalDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                workCenterMaintenance.setStartTime(date.atTime(9, 0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                workCenterMaintenance.setEndTime(date.atTime(17, 30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                workCenterMaintenance.setCapacity(480L);
                workCenterMaintenance.setDescription(baseWorkCenter.getDescription());
                workCenterMaintenances.add(workCenterMaintenance);
            }
            repository.saveAll(workCenterMaintenances);
        }
    }

    /**
     * 创建所有工作中心的工作日历
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 创建的工作日历数量
     */
    @Transactional("mysqlTransactionManager")
    public int createWorkCalendarForAllCenters(LocalDate startDate, LocalDate endDate) {
        // 直接获取所有工作中心信息，避免额外的查询
        List<MesBaseWorkCenter> allWorkCenters = mesBaseWorkCenterService.findAll();
        int totalCreated = 0;

        if (allWorkCenters == null || allWorkCenters.isEmpty()) {
            return 0;
        }

        for (MesBaseWorkCenter baseWorkCenter : allWorkCenters) {
            List<ApsWorkCenterMaintenance> workCenterMaintenances = new ArrayList<>();

            // 为每个工作中心在指定日期范围内创建工作日历
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                ApsWorkCenterMaintenance workCenterMaintenance = new ApsWorkCenterMaintenance();
                workCenterMaintenance.setId(RandomFun.getInstance().getRandom());
                workCenterMaintenance.setStatus("Active");
                workCenterMaintenance.setWorkCenterCode(baseWorkCenter.getWorkCenterCode());
                workCenterMaintenance.setLocalDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                workCenterMaintenance.setStartTime(date.atTime(9, 0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                workCenterMaintenance.setEndTime(date.atTime(17, 30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                workCenterMaintenance.setCapacity(480L);
                workCenterMaintenance.setDescription(baseWorkCenter.getDescription() + " - 工作日历");
                workCenterMaintenances.add(workCenterMaintenance);
            }

            // 批量保存当前工作中心的所有工作日历
            saveAll(workCenterMaintenances);
            totalCreated += workCenterMaintenances.size();
        }

        return totalCreated;
    }


    /**
     * 保存所有工作中心维护计划
     *
     * @param workCenterMaintenances 工作中心维护计划列表
     */

    @Transactional("mysqlTransactionManager")
    public void saveAll(List<ApsWorkCenterMaintenance> workCenterMaintenances) {
        repository.saveAll(workCenterMaintenances);
    }

    /**
     * 根据工作中心代码查询所有维护计划
     *
     * @param workCenterCodes 工作中心代码列表
     * @return 工作中心维护计划列表
     */
    public List<ApsWorkCenterMaintenance> findAllByWorkCenterCodeIn(List<String> workCenterCodes) {
        return repository.findAllByWorkCenterCodeIn(workCenterCodes);
    }
}
