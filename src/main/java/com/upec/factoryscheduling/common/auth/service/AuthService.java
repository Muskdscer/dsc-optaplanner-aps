package com.upec.factoryscheduling.common.auth.service;

import com.upec.factoryscheduling.common.auth.dto.LoginRequest;
import com.upec.factoryscheduling.common.auth.dto.LoginResponse;
import com.upec.factoryscheduling.common.auth.dto.RegisterRequest;
import com.upec.factoryscheduling.common.auth.entity.User;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册的用户
     */
    User register(RegisterRequest registerRequest);

    /**
     * 用户登出
     */
    void logout();
}
