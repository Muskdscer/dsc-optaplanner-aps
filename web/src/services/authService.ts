// 登录响应接口
export interface LoginResponse {
  success: boolean;
  message?: string;
  token?: string;
  user?: {
    username: string;
    name?: string;
    permissions?: string[];
  };
}

// 用户信息接口
export interface UserInfo {
  username: string;
  name?: string;
  permissions?: string[];
  isLoggedIn: boolean;
}

// 模拟登录API函数
export const login = async (username: string, password: string): Promise<LoginResponse> => {
  try {
    // 预留真实API调用接口，但目前使用本地验证
    // const response = await apiClient.post('/api/auth/login', { username, password });
    // return response;
    
    // 模拟API响应延迟
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 本地验证逻辑（写死的admin/123456）
    if (username === 'admin' && password === '123456') {
      const userInfo: UserInfo = {
        username: 'admin',
        name: '管理员',
        permissions: ['admin'],
        isLoggedIn: true
      };
      
      // 保存用户信息到localStorage
      localStorage.setItem('userInfo', JSON.stringify(userInfo));
      localStorage.setItem('isLoggedIn', 'true');
      
      return {
        success: true,
        message: '登录成功',
        token: 'mock-token-' + Date.now(),
        user: userInfo
      };
    } else {
      return {
        success: false,
        message: '用户名或密码错误'
      };
    }
  } catch (error) {
    console.error('登录错误:', error);
    return {
      success: false,
      message: '登录失败，请重试'
    };
  }
};

// 登出函数
export const logout = async (): Promise<void> => {
  try {
    // 预留真实API调用接口
    // await apiClient.post('/api/auth/logout');
    
    // 清除本地存储的用户信息
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    
    // 刷新页面或跳转到登录页
    window.location.href = '/login';
  } catch (error) {
    console.error('登出错误:', error);
    // 即使API调用失败，也清除本地状态
    localStorage.removeItem('userInfo');
    localStorage.removeItem('isLoggedIn');
    window.location.href = '/login';
  }
};

// 获取当前用户信息
export const getCurrentUser = (): UserInfo | null => {
  try {
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      return JSON.parse(userInfoStr);
    }
    return null;
  } catch (error) {
    console.error('获取用户信息错误:', error);
    return null;
  }
};

// 检查用户是否已登录
export const isLoggedIn = (): boolean => {
  try {
    const loggedIn = localStorage.getItem('isLoggedIn');
    return loggedIn === 'true';
  } catch (error) {
    console.error('检查登录状态错误:', error);
    return false;
  }
};
