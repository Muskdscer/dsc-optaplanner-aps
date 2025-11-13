package com.upec.factoryscheduling.common.auth.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private String phone;

    public LoginResponse(String token, String username, String email, String phone) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.phone = phone;
    }
}
