# 工厂调度系统API文档

本文档提供了工厂调度系统的REST API接口定义，支持Postman工具导入使用。

## 文件说明

- `factory_scheduling_api.postman_collection.json` - Postman Collection格式的API文档，可以直接导入到Postman工具中使用
- `factory_scheduling_api.apifox.json` - APIfox格式的API文档（可选）

## 如何使用Postman导入

1. 下载并安装 [Postman](https://www.postman.com/)
2. 打开Postman，点击左上角的「Import」按钮
3. 选择「Upload Files」，然后选择本目录下的 `factory_scheduling_api.postman_collection.json` 文件
4. 点击「Import」按钮完成导入
5. 导入完成后，可以在Collections面板中找到「工厂调度系统API」集合，包含所有API接口
6. 使用前请确保更新`baseUrl`变量为实际的API服务器地址（默认为`http://localhost:8080`）

## API接口分类

- **调度管理** - 提供调度相关的API接口
- **设备管理** - 提供工作中心（设备/机器）相关的API接口
- **维护管理** - 提供设备维护计划相关的API接口
- **订单管理** - 提供订单相关的API接口
- **时间槽管理** - 提供时间槽相关的API接口
- **工序管理** - 提供工序相关的API接口
- **工作日历管理** - 提供工作日历相关的API接口
- **MES订单管理** - 提供MES订单同步相关的API接口

## 注意事项

- 所有API接口都使用RESTful风格设计
- 接口响应统一使用 `ApiResponse` 格式
- 部分POST和PUT接口已包含示例请求数据，可以根据实际需求修改
- 使用前请确保API服务器已启动并运行正常