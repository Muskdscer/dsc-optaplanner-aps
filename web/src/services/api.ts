import axios from 'axios';
import type { AxiosResponse } from 'axios';
import type {
  OrderTaskQueryParams,
  Task,
  OrderTask,
  PageResponse,
  ApiResponse,
  SpringDataPage,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  UserInfo
} from './model';

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8082',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true // 允许跨域请求携带cookies
});

// 请求拦截器 - 添加JWT令牌
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data;
  },
  (error) => {
    console.error('API请求错误:', error);
    // 如果返回401未授权，清除本地存储并跳转到登录页
    if (error.response?.status === 401) {
      localStorage.removeItem('userInfo');
      localStorage.removeItem('isLoggedIn');
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 根据条件分页查询订单任务
export const queryOrderTasksWithPagination = async (params: OrderTaskQueryParams): Promise<PageResponse> => {
  try {
    // 构建查询参数
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
    throw error;
  }
};

// 同步订单数据到MES系统
export const syncOrderData = async (orderNos: string[]): Promise<void> => {
  try {
    const response:ApiResponse<string> = await apiClient.post('/api/mesOrders/syncData', orderNos);
    // 检查响应状态，确保同步成功
    if (response.code !== 200) {
      throw new Error(`同步失败: ${response.msg || '未知错误'}`);
    }
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
    // 保存服务器响应（响应拦截器已经返回了response.data）
    const response:ApiResponse<string> = await apiClient.post(`/api/scheduling/solve/${problemId}`, ordernos);    
    // 此时response已经是{code: 200, msg: "success", data: "..."}的格式
    if (response.code === 200) {
      return response.data || '';
    } else {
      throw new Error(`API调用失败: ${response.msg || '未知错误'}`);
    }
  } catch (error) {
    console.error('开始任务失败:', error);
    throw error;
  }
};

// ====================================
// 认证相关API函数
// ====================================

// 登录API函数
export const login = async (username: string, password: string): Promise<LoginResponse> => {
  try {
    // 调用真实API
    const response:ApiResponse<LoginResponse> = await apiClient.post('/api/auth/login', { username, password });
    console.log('登录响应:', response);
    if (response.code === 200) {
      const userInfo: UserInfo = {
        username: response.data.username,
        name: response.data.username,
        permissions: ['user'],
        isLoggedIn: true
      };
      // 保存用户信息到localStorage
      localStorage.setItem('userInfo', JSON.stringify(userInfo));
      localStorage.setItem('isLoggedIn', 'true');
      localStorage.setItem('token', response.data.token);
      
      return response.data;
    } else {
      throw new Error(response.msg || '登录失败');
    }
  } catch (error) {
    console.error('登录错误:', error);
    throw error;
  }
};

// 注册API函数
export const register = async (registerData: RegisterRequest): Promise<RegisterResponse> => {
  try {
    const response:ApiResponse<RegisterResponse> = await apiClient.post('/api/auth/register', registerData);
    // 此时response已经是{code: number, msg: string, data: string}的格式
    return response.data;
  } catch (error) {
    console.error('注册错误:', error);
    throw new Error('注册失败，请重试');
  }
};

// 登出API函数
export const logout = async (): Promise<void> => {
  try {
    // 调用真实API
    await apiClient.post('/api/auth/logout');
    // 清除本地存储的用户信息
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('token');
    
    // 刷新页面或跳转到登录页
    window.location.href = '/login';
  } catch (error) {
    console.error('登出错误:', error);
    // 即使API调用失败，也清除本地状态
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
};

// 创建时间槽
export const createTimeslot = async (taskNos: string[], procedureIds: string[], time: number = 0.5, slice: number = 0): Promise<void> => {
  try {
    const params = new URLSearchParams();
    // 添加taskNos参数
    taskNos.forEach(taskNo => params.append('taskNos', taskNo));
    if(taskNos.length === 0) {
      params.append('taskNos', '');
    }
    // 添加procedureIds参数
    procedureIds.forEach(procedureId => params.append('procedureIds', procedureId));
    // 添加其他参数，添加空值检查
    if(procedureIds.length === 0) {
      params.append('procedureIds', '');
    }
    params.append('time', time !== null && time !== undefined ? time.toString() : '0.5');
    params.append('slice', slice !== null && slice !== undefined ? slice.toString() : '0');
    
    const response: ApiResponse<void> = await apiClient.post('/api/timeslot/create', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    });
    
    if (response.code !== 200) {
      throw new Error(`创建时间槽失败: ${response.msg || '未知错误'}`);
    }
  } catch (error) {
    console.error('创建时间槽失败:', error);
    throw error;
  }
};
