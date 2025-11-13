import axios from 'axios';
import type { AxiosResponse } from 'axios';
import type { OrderTaskQueryParams, Task, OrderTask, PageResponse, ApiResponse, SpringDataPage } from './model';

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data;
  },
  (error) => {
    console.error('API请求错误:', error);
    return Promise.reject(error);
  }
);

// 根据条件分页查询订单任务
export const queryOrderTasksWithPagination = async (params: OrderTaskQueryParams): Promise<PageResponse> => {
  try {
    // 构建查询参数
    console.log('查询参数:', params);
    const queryParams = {
      orderName: params.orderName,
      startTime: params.startTime,
      endTime: params.endTime,
      statusList: params.statusList?.join(','),
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20,
      orderNo: params.orderNo || '',
      taskNo: params.taskNo || '',
      taskStatus: params.taskStatus || ''
    };
    
    // 由于响应拦截器已经返回response.data，直接获取ApiResponse格式的数据
    const result: ApiResponse<SpringDataPage<OrderTask>> = await apiClient.get('/api/mesOrders/orderTasks/page', { params: queryParams });
    
    // 确保返回的数据格式正确
    if (!result || result.code !== 200) {
      throw new Error(`API调用失败: ${result?.msg || '未知错误'}`);
    }
    
    const pageData = result.data;
    
    // 转换Spring Data Page为前端需要的格式
    return {
      total: pageData.totalElements,
      records: pageData.content || [],
      totalPages: pageData.totalPages,
      pageSize: pageData.size,
      pageNum: pageData.number + 1 // Spring Data的页码从0开始，转换为前端从1开始
    };
  } catch (error) {
    console.error('分页查询订单任务失败:', error);
    throw error;
  }
};

// 同步订单数据到MES系统
export const syncOrderData = async (orderNos: string[]): Promise<void> => {
  try {
    await apiClient.post('/api/mesOrders/syncData', orderNos);
    console.log('同步订单数据成功，订单编号:', orderNos);
  } catch (error) {
    console.error('同步订单数据失败:', error);
    throw error;
  }
};

// 调用OrderController.queryTasks接口获取任务数据
export const queryTasks = async (params?: OrderTaskQueryParams): Promise<Task[]> => {
  try {
    // 构建查询参数
    const queryParams = {
      startTime: params?.startTime,
      endTime: params?.endTime,
      taskNo: params?.taskNo,
      taskStatus: params?.taskStatus
    };
    
    // 调用API接口
    console.log('查询任务参数:', queryParams);
    
    // 适配新的API返回格式：ApiResponse<SpringDataPage<Task>>
    const result: ApiResponse<SpringDataPage<Task>> = await apiClient.get('/api/orders/tasks', { params: queryParams });
    
    // 确保返回的数据格式正确
    if (!result || result.code !== 200) {
      throw new Error(`API调用失败: ${result?.msg || '未知错误'}`);
    }
    
    // 从嵌套结构中提取任务列表
    const tasks = result.data?.content || [];
    console.log('查询任务成功，返回数据长度:', tasks.length);
    return tasks;
  } catch (error) {
    console.error('查询任务失败:', error);
    throw error;
  }
};

// 开始任务调度
export const startTasks = async (ordernos: string[]): Promise<string> => {
  try {
    // 检查订单编号列表是否为空
    if (!ordernos || ordernos.length === 0) {
      throw new Error('订单编号列表不能为空');
    }

    // 根据时间戳生成problemId
    const problemId = `${Date.now()}`;

    // 调用API接口
    console.log('开始任务调度，订单编号:', ordernos, 'problemId:', problemId);
    
    // 保存服务器响应（响应拦截器已经返回了response.data）
    const response = await apiClient.post(`/api/scheduling/solve/${problemId}`, ordernos);
    
    // 使用axios实例处理响应
    console.log('任务开始成功，响应数据:', response);
    
    // 直接返回响应中的data字段（此时response已经是{code: 200, msg: "success", data: "..."}的格式）
    return response?.data || '';
  } catch (error) {
    console.error('开始任务失败:', error);
    throw error;
  }
};
