package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.auth.PasswordChangeRequest;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostVO;
import com.techhub.dto.user.UserDetailVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.dto.user.UserUpdateRequest;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("UserController 单元测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        // Mock authenticated user with userId=1
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== GET /users/me ====================

    @Test
    @DisplayName("getCurrentUser — 返回当前用户详情")
    void getCurrentUser_ReturnsUserDetail() throws Exception {
        UserDetailVO detail = new UserDetailVO();
        detail.setId("1");
        detail.setUsername("testuser");
        detail.setEmail("test@example.com");
        detail.setAvatarUrl("https://example.com/avatar.png");
        detail.setBio("Hello world");
        detail.setRole("USER");
        detail.setStatus(1);
        detail.setCreateTime(LocalDateTime.now());
        detail.setUpdateTime(LocalDateTime.now());

        when(userService.getCurrentUser()).thenReturn(detail);

        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    // ==================== PATCH /users/me ====================

    @Test
    @DisplayName("updateProfile — 更新个人信息成功")
    void updateProfile_Success() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setBio("new bio");

        doNothing().when(userService).updateProfile(any(UserUpdateRequest.class));

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));
    }

    // ==================== GET /users/{id} ====================

    @Test
    @DisplayName("getUserProfile — 返回公开信息，不含 email")
    void getUserProfile_ReturnsPublicInfo() throws Exception {
        UserProfileVO profile = new UserProfileVO();
        profile.setId("2");
        profile.setUsername("otheruser");
        profile.setAvatarUrl("https://example.com/avatar2.png");
        profile.setBio("Another person");
        profile.setRole("USER");
        profile.setCreateTime(LocalDateTime.now());

        when(userService.getUserProfile(2L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("2"))
                .andExpect(jsonPath("$.data.username").value("otheruser"))
                .andExpect(jsonPath("$.data.email").doesNotExist());
    }

    @Test
    @DisplayName("getUserProfile — 用户不存在返回 404")
    void getUserProfile_NotFound_Returns404() throws Exception {
        when(userService.getUserProfile(999L))
                .thenThrow(new BusinessException(ResultCode.NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== GET /users/{id}/posts ====================

    @Test
    @DisplayName("getUserPosts — 返回分页帖子列表")
    void getUserPosts_ReturnsPaginated() throws Exception {
        PostVO post = new PostVO();
        post.setId("100");
        post.setTitle("Test Post");
        post.setAuthorId("1");
        post.setAuthorName("testuser");
        post.setCreateTime(LocalDateTime.now());

        PageResult<PostVO> pageResult = PageResult.of(List.of(post), 1, 10, 1);

        when(userService.getUserPosts(eq(1L), any(PostListQuery.class), eq(1L)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/users/1/posts")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").exists())
                .andExpect(jsonPath("$.data.records[0].id").value("100"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    // ==================== PATCH /users/me/password ====================

    @Test
    @DisplayName("changePassword — 修改密码成功")
    void changePassword_Success() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("oldPass123");
        request.setNewPassword("newPass456");

        doNothing().when(userService).changePassword(eq(1L), eq("oldPass123"), eq("newPass456"));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("密码修改成功"));
    }

    @Test
    @DisplayName("changePassword — 原密码错误返回 400")
    void changePassword_WrongOldPassword_Returns400() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("wrongOld");
        request.setNewPassword("newPass456");

        doThrow(new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确"))
                .when(userService).changePassword(eq(1L), eq("wrongOld"), eq("newPass456"));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("原密码不正确"));
    }

    @Test
    @DisplayName("changePassword — 新密码过短返回 400")
    void changePassword_ShortNewPassword_Returns400() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("oldPass123");
        request.setNewPassword("a");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("密码长度需在6-100个字符之间")));
    }

    // ==================== GET /users/me/favorites ====================

    @Test
    @DisplayName("getFavorites — 返回分页收藏列表")
    void getFavorites_ReturnsPaginated() throws Exception {
        PostVO post = new PostVO();
        post.setId("200");
        post.setTitle("Favorited Post");
        post.setAuthorId("1");
        post.setCreateTime(LocalDateTime.now());

        PageResult<PostVO> pageResult = PageResult.of(List.of(post), 1, 10, 1);

        when(userService.getFavorites(eq(1L), anyInt(), anyInt())).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").exists())
                .andExpect(jsonPath("$.data.records[0].id").value("200"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("getFavorites — 收藏为空时返回空列表")
    void getFavorites_Empty_ReturnsEmptyList() throws Exception {
        PageResult<PostVO> emptyResult = PageResult.of(List.of(), 0, 10, 1);

        when(userService.getFavorites(eq(1L), anyInt(), anyInt())).thenReturn(emptyResult);

        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
