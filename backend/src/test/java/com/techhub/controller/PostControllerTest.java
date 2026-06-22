package com.techhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostCreateRequest;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostUpdateRequest;
import com.techhub.dto.post.PostVO;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.PostService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PostController 单元测试")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    private PostVO mockPost;

    @BeforeEach
    void setUp() {
        mockPost = new PostVO();
        mockPost.setId("1");
        mockPost.setTitle("Test Post");
        mockPost.setContent("Test content");
        mockPost.setCategoryId("10");
        mockPost.setCategoryName("Java");
        mockPost.setAuthorId("1");
        mockPost.setAuthorName("testuser");
        mockPost.setVisibility(0);
        mockPost.setViewCount(100);
        mockPost.setLikeCount(10);
        mockPost.setCommentCount(5);
        mockPost.setDivineCommentCount(1);
        mockPost.setLiked(false);
        mockPost.setFavorited(false);
        mockPost.setCreateTime(LocalDateTime.now());
        mockPost.setUpdateTime(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    // ==================== GET /api/v1/posts ====================

    @Test
    @DisplayName("listPosts — 返回公开帖子列表")
    void listPosts_ReturnsPublicPosts() throws Exception {
        PageResult<PostVO> pageResult = PageResult.of(List.of(mockPost), 1, 20, 1);

        when(postService.listPosts(any(PostListQuery.class), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("1"))
                .andExpect(jsonPath("$.data.records[0].title").value("Test Post"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    @DisplayName("listPosts — page<1 返回 400")
    void listPosts_PageLessThan1_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("listPosts — size>50 由 Service 层兜底返回 200")
    void listPosts_SizeExceeds50_Returns200() throws Exception {
        PageResult<PostVO> pageResult = PageResult.of(List.of(mockPost), 50, 50, 1);

        when(postService.listPosts(any(PostListQuery.class), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("listPosts — 合法分页参数返回 200")
    void listPosts_ValidPagination_Returns200() throws Exception {
        PageResult<PostVO> pageResult = PageResult.of(List.of(mockPost), 1, 10, 1);

        when(postService.listPosts(any(PostListQuery.class), eq(null)))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== POST /api/v1/posts ====================

    @Test
    @DisplayName("createPost — 成功创建帖子返回 200")
    void createPost_Creates_Returns200() throws Exception {
        authenticateUser();

        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("New Post");
        request.setContent("New content");
        request.setCategoryId(10L);
        request.setVisibility(0);

        PostVO created = new PostVO();
        created.setId("100");
        created.setTitle("New Post");
        created.setContent("New content");
        created.setCategoryId("10");
        created.setAuthorId("1");
        created.setAuthorName("testuser");
        created.setVisibility(0);
        created.setCreateTime(LocalDateTime.now());

        when(postService.createPost(any(PostCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("100"))
                .andExpect(jsonPath("$.data.title").value("New Post"));
    }

    @Test
    @DisplayName("createPost — 未认证返回 401")
    void createPost_Unauthenticated_Returns401() throws Exception {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("New Post");
        request.setContent("New content");
        request.setCategoryId(10L);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== GET /api/v1/posts/{id} ====================

    @Test
    @DisplayName("getPostDetail — 返回帖子详情")
    void getPostDetail_ReturnsPost() throws Exception {
        when(postService.getPostDetail(eq(1L), eq(null))).thenReturn(mockPost);

        mockMvc.perform(get("/api/v1/posts/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.authorName").value("testuser"))
                .andExpect(jsonPath("$.data.viewCount").value(100));
    }

    @Test
    @DisplayName("getPostDetail — 帖子不存在返回 404")
    void getPostDetail_NotFound_Returns404() throws Exception {
        when(postService.getPostDetail(eq(999L), eq(null)))
                .thenThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"));

        mockMvc.perform(get("/api/v1/posts/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("帖子不存在"));
    }

    // ==================== DELETE /api/v1/posts/{id} ====================

    @Test
    @DisplayName("deletePost — 作者删除帖子成功")
    void deletePost_AsAuthor_Succeeds() throws Exception {
        authenticateUser();

        doNothing().when(postService).deletePost(1L);

        mockMvc.perform(delete("/api/v1/posts/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }
}
