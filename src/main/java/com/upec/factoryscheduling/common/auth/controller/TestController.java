package com.upec.factoryscheduling.common.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // 公开端点，无需认证即可访问
    @GetMapping("/public")
    public ResponseEntity<?> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "这是一个公开的端点，可以自由访问");
        return ResponseEntity.ok(response);
    }

    // 受保护端点，需要认证才能访问
    @GetMapping("/protected")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> protectedEndpoint() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, String> response = new HashMap<>();
        response.put("message", "这是一个受保护的端点，只有认证用户才能访问");
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities().toString());
        return ResponseEntity.ok(response);
    }
}