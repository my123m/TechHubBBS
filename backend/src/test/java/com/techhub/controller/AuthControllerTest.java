package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.auth.LoginRequest;
import com.techhub.dto.auth.LoginResponse;
import com.techhub.dto.auth.RegisterRequest;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    // ==================== Register Tests ====================

    @Test
    @DisplayName("register — 成功注册返回 200")
    void register_Success_Returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("newuser@example.com");

        doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    @Test
    @DisplayName("register — 用户名重复返回 409")
    void register_DuplicateUsername_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("new@example.com");

        doThrow(new BusinessException(ResultCode.CONFLICT, "用户名已存在"))
                .when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("register — 邮箱已被注册返回 409")
    void register_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("existing@example.com");

        doThrow(new BusinessException(ResultCode.CONFLICT, "邮箱已被注册"))
                .when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("邮箱已被注册"));
    }

    @Test
    @DisplayName("register — 无效邮箱 notanemail 返回 400")
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testemail1");
        request.setPassword("password123");
        request.setEmail("notanemail");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("邮箱格式不正确")));
    }

    @Test
    @DisplayName("register — 无 TLD 邮箱 a@b 返回 400")
    void register_EmailWithoutTld_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testemail2");
        request.setPassword("password123");
        request.setEmail("a@b");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("邮箱格式不正确")));
    }

    @Test
    @DisplayName("register — 邮箱为空时注册成功")
    void register_NoEmail_Returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("noemailuser");
        request.setPassword("password123");

        doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    // ==================== Login Tests ====================

    @Test
    @DisplayName("login — 登录成功返回 Token")
    void login_Success_ReturnsToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken("mock-jwt-token");
        loginResponse.setTokenType("Bearer");
        loginResponse.setUserId("1");
        loginResponse.setUsername("testuser");
        loginResponse.setRole("USER");

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.userId").value("1"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("login — 密码错误返回 401")
    void login_WrongPassword_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        doThrow(new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"))
                .when(authService).login(any(LoginRequest.class));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("login — 被封禁用户返回 403")
    void login_BannedUser_Returns403() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("banneduser");
        request.setPassword("password123");

        doThrow(new BusinessException(ResultCode.FORBIDDEN, "用户已被封禁"))
                .when(authService).login(any(LoginRequest.class));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("用户已被封禁"));
    }
}
