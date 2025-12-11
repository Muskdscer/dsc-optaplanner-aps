# 工厂调度系统 (OptaPlanner APS)

一个基于 Spring Boot 和 OptaPlanner 的高级计划与调度系统，用于解决复杂的工厂生产调度问题。

## 系统概述

本系统利用 OptaPlanner 的约束求解能力，为工厂生产提供智能调度解决方案。系统考虑了订单优先级、设备可用性、工序依赖关系等多种约束条件，生成最优的生产排期计划。

### 主要功能特点

- **自动调度优化**：基于约束条件自动生成最优调度方案
- **多约束处理**：支持订单优先级、设备维护、工序顺序等多种约束
- **实时调度调整**：支持动态插入新订单并重新优化调度
- **灵活的资源管理**：支持工作中心（设备）的动态配置和维护管理
- **可视化API接口**：提供完整的REST API，方便与其他系统集成

## 系统架构

### 技术栈

- **后端框架**：Spring Boot 2.6.13
- **规则引擎**：OptaPlanner 8.44.0.Final
- **数据库**：H2（开发测试）、MySQL（生产）
- **ORM框架**：Hibernate/JPA
- **API设计**：RESTful API

### 核心模块

1. **aps模块**：高级计划与调度核心功能
   - entity: 核心实体类（订单、工序、工作中心、时间槽等）
   - repository: 数据访问层
   - service: 业务逻辑层
   - controller: REST API接口
   - solution: OptaPlanner解决方案相关类

2. **mes模块**：制造执行系统集成
   - 提供与MES系统的数据交换功能

3. **solver模块**：调度求解器
   - 定义调度约束条件和优化目标

## 快速开始

### 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0+（可选，默认使用H2）

### 构建与运行

```bash
# 克隆仓库（如果适用）
# git clone [repository-url]

# 进入项目目录
cd optaplanner-aps

# 构建项目
mvn clean package

# 运行应用
java -jar target/example-0.0.1-SNAPSHOT.jar
```

应用启动后，将在 http://localhost:8080 提供服务。

## API接口说明

### 1. 资源管理接口

#### 工作中心（设备）管理

- `POST /api/machines` - 批量创建工作中心
- `GET /api/machines` - 获取所有工作中心
- `GET /api/machines/{id}` - 根据ID获取工作中心
- `PUT /api/machines/{id}` - 更新工作中心信息
- `DELETE /api/machines/{id}` - 删除工作中心

#### 订单管理

- `POST /api/orders` - 批量创建订单
- `GET /api/orders` - 获取所有订单
- `GET /api/orders/{id}` - 根据ID获取订单
- `PUT /api/orders/{id}` - 更新订单信息
- `DELETE /api/orders/{id}` - 删除订单

#### 工序管理

- `POST /api/procedures` - 批量创建工序
- `GET /api/procedures` - 获取所有工序
- `GET /api/procedures/{id}` - 根据ID获取工序
- `PUT /api/procedures/{id}` - 更新工序信息
- `DELETE /api/procedures/{id}` - 删除工序

#### 设备维护管理

- `POST /api/machine-maintenance` - 批量创建设备维护计划
- `GET /api/machine-maintenance` - 获取所有维护计划
- `GET /api/machine-maintenance/{id}` - 根据ID获取维护计划
- `PUT /api/machine-maintenance/{id}` - 更新维护计划
- `DELETE /api/machine-maintenance/{id}` - 删除维护计划

### 2. 调度相关接口

- `POST /api/scheduling/solve/{problemId}` - 启动调度求解
- `POST /api/scheduling/stop/{problemId}` - 停止调度求解
- `GET /api/scheduling/solution/{problemId}` - 获取最佳调度解决方案
- `GET /api/scheduling/status/{problemId}` - 获取调度状态

## 使用客户端示例

项目中提供了一个完整的客户端示例，演示如何使用API进行工厂调度：

```java
// 运行客户端示例
java -cp target/example-0.0.1-SNAPSHOT.jar com.upec.factoryscheduling.client.FactorySchedulingClient
```

客户端示例演示了完整的调度流程：
1. 创建工作中心
2. 创建订单
3. 创建工序
4. 创建设备维护计划（可选）
5. 启动调度
6. 等待调度完成
7. 获取并分析调度结果

## 数据模型说明

### 核心实体关系

- **订单(Order)**：包含多个工序(Procedure)，有优先级属性
- **工序(Procedure)**：属于某个订单，必须在特定工作中心(WorkCenter)上执行，有顺序依赖关系
- **工作中心(WorkCenter)**：表示生产设备，有可用状态
- **时间槽(Timeslot)**：调度的基本单元，关联工序、工作中心和时间安排
- **工作中心维护(WorkCenterMaintenance)**：表示设备的维护计划，维护期间设备不可用

## 调度约束说明

系统实现了以下主要约束：

### 硬约束（必须满足）
- 工作中心时间冲突约束：同一时间同一设备只能执行一个工序
- 工序顺序约束：必须按照预定顺序执行工序
- 工作中心维护冲突约束：设备维护期间不可用
- 固定开始时间约束：某些工序可能有固定的开始时间要求

### 软约束（尽量满足）
- 订单优先级最大化：高优先级订单优先安排
- 机器利用率最大化：提高设备利用率
- 制造周期最小化：缩短整体生产时间
- 工序分片连续性偏好：尽量连续执行同一工序的分片

## 扩展指南

### 添加新约束

要添加新的调度约束，只需在 `FactorySchedulingConstraintProvider` 类中添加新的约束方法，并在 `defineConstraints` 方法中注册：

```java
private Constraint myNewConstraint(ConstraintFactory constraintFactory) {
    return constraintFactory.forEach(Timeslot.class)
            .filter(/* 过滤条件 */)
            .penalize("My New Constraint", HardSoftScore.ONE_SOFT);
}
```

### 集成其他系统

系统提供了mes模块，用于与制造执行系统(MES)集成。可以通过扩展mes模块，实现与其他系统的数据交换。

## 常见问题解答

1. **调度结果不可行怎么办？**
   - 检查约束条件是否过于严格
   - 考虑调整订单优先级或放宽某些非关键约束
   - 增加可用资源（工作中心）

2. **调度求解时间过长怎么办？**
   - 可以设置求解时间限制
   - 减少同时调度的订单数量
   - 检查是否有不必要的复杂约束

3. **如何处理紧急插入的订单？**
   - 使用现有API重新启动调度，包含新订单
   - 可以为紧急订单设置高优先级

## 许可证

[在此添加许可证信息]

## 联系方式

[在此添加联系方式]