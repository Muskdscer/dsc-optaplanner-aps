package com.upec.factoryscheduling.auth.controller;

import com.upec.factoryscheduling.auth.dto.LoginRequest;
import com.upec.factoryscheduling.auth.dto.LoginResponse;
import com.upec.factoryscheduling.auth.dto.RegisterRequest;
import com.upec.factoryscheduling.auth.service.AuthService;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ApiResponse.ok(response);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            return ApiResponse.ok("注册成功");
        } catch (RuntimeException e) {
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        authService.logout();
        return ApiResponse.ok("登出成功");
    }
}
