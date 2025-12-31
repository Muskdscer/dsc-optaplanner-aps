package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.repository.WorkCenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 工作中心服务类
 * <p>负责管理工厂中所有工作中心(设备/机器)的CRUD操作和同步功能，
 * 为工厂调度系统提供工作中心资源的数据支持。</p>
 */
@Service
public class WorkCenterService {

    /** 工作中心数据访问层 - 提供工作中心实体的持久化操作 */
    private WorkCenterRepository workCenterRepository;

    /**
     * 设置工作中心仓库
     * @param workCenterRepository 工作中心数据访问层实例
     */
    @Autowired
    private void setMachineRepository(WorkCenterRepository workCenterRepository) {
        this.workCenterRepository = workCenterRepository;
    }

    /**
     * 获取所有工作中心
     * @return 工作中心列表，包含系统中所有的设备/机器信息
     */
    public List<WorkCenter> getAllMachines() {
        return workCenterRepository.findAll();
    }

    /**
     * 根据ID获取工作中心
     * @param id 工作中心ID
     * @return 包含工作中心的Optional对象，若不存在则为空
     */
    public Optional<WorkCenter> getMachineById(String id) {
        return workCenterRepository.findById(id);
    }

    /**
     * 更新工作中心信息
     * @param id 工作中心ID
     * @param workCenter 包含更新信息的工作中心对象
     * @return 更新后的工作中心实体，若不存在则返回null
     */
    @Transactional("oracleTransactionManager")
    public WorkCenter updateMachine(String id, WorkCenter workCenter) {
        Optional<WorkCenter> machine = workCenterRepository.findById(id);
        if (machine.isPresent()) {
            WorkCenter existingMachine = machine.get();
            existingMachine.setName(workCenter.getName());
            return workCenterRepository.save(existingMachine);
        }
        return null;
    }

    /**
     * 删除工作中心
     * @param id 要删除的工作中心ID
     */
    @Transactional("oracleTransactionManager")
    public void deleteMachine(String id) {
        workCenterRepository.deleteById(id);
    }

    /**
     * 批量保存工作中心
     * @param workCenters 工作中心列表
     * @return 保存后的工作中心列表
     */
    @Transactional("oracleTransactionManager")
    public List<WorkCenter> saveWorkCenters(List<WorkCenter> workCenters) {
        return workCenterRepository.saveAll(workCenters);
    }

    /**
     * 创建多个工作中心
     * @param machines 工作中心列表
     * @return 创建后的工作中心列表
     */
    @Transactional("oracleTransactionManager")
    public List<WorkCenter> create(List<WorkCenter> machines) {
        return workCenterRepository.saveAll(machines);
    }


    /**
     * 清空所有工作中心数据
     * <p>使用事务保证原子性操作</p>
     */
    @Transactional("oracleTransactionManager")
    public void deleteAll() {
        workCenterRepository.deleteAll();
    }

}
