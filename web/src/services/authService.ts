// 重新导出所有认证相关类型定义
export type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  UserInfo
} from './model';

// 重新导出所有认证相关API函数
export {
  login,
  register,
  logout
} from './api';

// 获取当前用户信息
export const getCurrentUser = (): any | null => {
  try {
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      return JSON.parse(userInfoStr);
    }
    return null;
  } catch (error) {

    return null;
  }
};

// 检查用户是否已登录
export const isLoggedIn = (): boolean => {
  try {
    const loggedIn = localStorage.getItem('isLoggedIn');
    return loggedIn === 'true';
  } catch (error) {

    return false;
  }
};

// 清除用户登录状态（不调用API）
export const clearUserSession = (): void => {
  localStorage.removeItem('userInfo');
  localStorage.removeItem('isLoggedIn');
  localStorage.removeItem('token');
};

// 保存用户登录状态
export const saveUserSession = (userInfo: any, token: string): void => {
  localStorage.setItem('userInfo', JSON.stringify(userInfo));
  localStorage.setItem('isLoggedIn', 'true');
  localStorage.setItem('token', token);
};
