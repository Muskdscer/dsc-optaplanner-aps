package com.upec.factoryscheduling.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upec.factoryscheduling.common.auth.dto.LoginRequest;
import com.upec.factoryscheduling.common.auth.dto.RegisterRequest;
import com.upec.factoryscheduling.common.auth.entity.User;
import com.upec.factoryscheduling.common.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        // 清除测试数据
        userRepository.deleteAll();
    }

    @Test
    public void testPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("这是一个公开的端点，可以自由访问"));
    }

    @Test
    public void testProtectedEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUserRegistration() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPhone("13800138000");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        // 验证用户是否已保存到数据库
        User user = userRepository.findByUsername("testuser").orElseThrow();
        assert user.getEmail().equals("test@example.com");
        assert passwordEncoder.matches("password123", user.getPassword());
    }

    @Test
    public void testUserLogin() throws Exception {
        // 先注册用户
        User user = new User();
        user.setUsername("testloginuser");
        user.setPassword(passwordEncoder.encode("login123"));
        user.setEmail("login@example.com");
        user.setPhone("13900139000");
        user.setStatus(1);  // 1表示启用状态
        userRepository.save(user);

        // 登录测试
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testloginuser");
        loginRequest.setPassword("login123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn();

        // 提取 token
        String token = result.getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(token).get("token").asText();

        // 使用 token 访问受保护端点
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("这是一个受保护的端点，只有认证用户才能访问"))
                .andExpect(jsonPath("$.username").value("testloginuser"));
    }
}