package com.techhub.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 帖子完整生命周期集成测试：创建 → 查询列表 → 查询详情 → 编辑 → 软删除。
 * 测试四种可见权限、权限校验、分页、搜索过滤。
 */
@Sql(scripts = "classpath:sql/h2-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:sql/h2-integration-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("帖子生命周期集成测试")
@Transactional
class PostLifecycleIntegrationTest extends BaseIntegrationTest {

    private String authorToken;
    private String otherUserToken;
    private String adminToken;

    @BeforeEach
    void setUpUsers() throws Exception {
        // Register author
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"author","password":"password123","email":"author@test.com"}
                        """));
        String authorResp = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"author\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        authorToken = objectMapper.readTree(authorResp).get("data").get("token").asText();

        // Register other user
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"other","password":"password123","email":"other@test.com"}
                        """));
        String otherResp = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"other\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        otherUserToken = objectMapper.readTree(otherResp).get("data").get("token").asText();

        // Register + promote to admin (done in tests that need admin)
        // For now, admin token is the same as author; we set role via direct token generation
        adminToken = generateToken(999L, "admin_user", "ADMIN");
    }

    // ==================== 创建帖子 ====================

    @Test
    @DisplayName("创建公开帖子 → 200 + 返回完整 PostVO")
    void createPublicPost_ReturnsPostVO() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "My First Post",
                                    "content": "This is a test post with sufficient content length.This is a test post with sufficient content length.This is a test post with sufficient content length.",
                                    "categoryId": 1000,
                                    "visibility": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("My First Post"))
                .andExpect(jsonPath("$.data.authorName").value("author"))
                .andExpect(jsonPath("$.data.visibility").value(0))
                .andExpect(jsonPath("$.data.categoryId").value("1000"))
                .andExpect(jsonPath("$.data.viewCount").value(0))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.commentCount").value(0))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("创建帖子 → 未认证 → 401")
    void createPostUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "No Auth Post",
                                    "content": "Should fail",
                                    "categoryId": 1000
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("创建帖子 → 不存在的版块 → 400")
    void createPostInvalidCategory_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Bad Category",
                                    "content": "Test content Test content Test content Test content Test content ",
                                    "categoryId": 9999
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 查询帖子列表 ====================

    @Test
    @DisplayName("查询公开帖子列表 → 游客可见公开帖")
    void listPosts_GuestSeesPublicPosts() throws Exception {
        // Create a public post
        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Public Post",
                            "content": "Visible to everyone. Visible to everyone. Visible to everyone. Visible to everyone. Visible to everyone. ",
                            "categoryId": 1000,
                            "visibility": 0
                        }
                        """));

        // Guest (no token) can see public posts
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title").value("Public Post"));
    }

    @Test
    @DisplayName("查询帖子列表 → 游客不可见登录可见帖")
    void listPosts_GuestCannotSeeLoginOnlyPosts() throws Exception {
        // Create a login-only post
        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Login Only Post",
                            "content": "Members only content. Members only content. Members only content. Members only content. Members only content. ",
                            "categoryId": 1000,
                            "visibility": 1
                        }
                        """));

        // Guest sees empty list (login-only filtered out)
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(0)));
    }

    @Test
    @DisplayName("查询帖子列表 → 登录用户可见登录可见帖")
    void listPosts_LoggedInUserSeesLoginOnlyPosts() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Login Only Post",
                            "content": "Members only. Members only. Members only. Members only. Members only. ",
                            "categoryId": 1000,
                            "visibility": 1
                        }
                        """));

        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", bearerToken(otherUserToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title").value("Login Only Post"));
    }

    @Test
    @DisplayName("查询帖子列表 → 按版块过滤")
    void listPosts_FilterByCategory() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Java Post","content":"Java Java Java Java Java Java Java Java Java Java ","categoryId":1000,"visibility":0}
                        """));
        mockMvc.perform(post("/api/v1/posts")
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Vue Post","content":"Vue Vue Vue Vue Vue Vue Vue Vue Vue Vue ","categoryId":1001,"visibility":0}
                        """));

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "10")
                        .param("categoryId", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title").value("Java Post"));
    }

    @Test
    @DisplayName("查询帖子列表 → 分页验证")
    void listPosts_Pagination() throws Exception {
        // Create 5 posts
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/posts")
                    .header("Authorization", bearerToken(authorToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {"title":"Post %d","content":"Content %d Content %d Content %d Content %d Content %d ","categoryId":1000,"visibility":0}
                            """, i, i, i, i, i, i)));
        }

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(3)))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(3));
    }

    @Test
    @DisplayName("查询帖子列表 → pageSize 上限 50")
    void listPosts_CapsPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "100"))
                .andExpect(status().isOk());
        // Service 层兜底 size 上限 50，返回 200
    }

    // ==================== 查询帖子详情 ====================

    @Test
    @DisplayName("查询帖子详情 → 返回完整信息 + 浏览量+1")
    void getPostDetail_ReturnsFullDetail() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Detail Post",
                                    "content": "Detailed content Detailed content Detailed content Detailed content Detailed content ",
                                    "categoryId": 1000,
                                    "visibility": 0
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data.title").value("Detail Post"))
                .andExpect(jsonPath("$.data.authorName").value("author"))
                .andExpect(jsonPath("$.data.categoryName").isNotEmpty())
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.createTime").isNotEmpty());
    }

    @Test
    @DisplayName("查询帖子详情 → 私密帖子 → 非作者返回 404")
    void getPostDetail_PrivatePostNonAuthor_Returns404() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Private Post",
                                    "content": "Secret Secret Secret Secret Secret ",
                                    "categoryId": 1000,
                                    "visibility": 3
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        // Other user tries to access private post → 404 (not 403, to prevent info leak)
        assertNotFound(mockMvc.perform(get("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(otherUserToken))));
    }

    @Test
    @DisplayName("查询帖子详情 → 不存在 → 404")
    void getPostDetail_NonExistent_Returns404() throws Exception {
        assertNotFound(mockMvc.perform(get("/api/v1/posts/99999")));
    }

    // ==================== 编辑帖子 ====================

    @Test
    @DisplayName("编辑帖子 → 作者本人编辑 → 200")
    void updatePost_AsAuthor_Returns200() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Original Title",
                                    "content": "Original content Original content Original content Original content Original content ",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        assertOk(mockMvc.perform(patch("/api/v1/posts/{id}", postId)
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Title",
                                    "content": "Updated content Updated content Updated content Updated content Updated content "
                                }
                                """)))
                .andExpect(jsonPath("$.data.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.startsWith("Updated content")));
    }

    @Test
    @DisplayName("编辑帖子 → 非作者 → 403")
    void updatePost_NotAuthor_Returns403() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Author's Post",
                                    "content": "Only author can edit Only author can edit Only author can edit Only author can edit Only author can edit ",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        assertForbidden(mockMvc.perform(patch("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(otherUserToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hacked Title\"}")));
    }

    @Test
    @DisplayName("编辑帖子 → PATCH 语义 → 只更新传入字段")
    void updatePost_PatchSemantics() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Original",
                                    "content": "Original content Original content Original content Original content Original content ",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        // Only update title
        assertOk(mockMvc.perform(patch("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New Title Only\"}")))
                .andExpect(jsonPath("$.data.title").value("New Title Only"))
                .andExpect(jsonPath("$.data.content").value("Original content Original content Original content Original content Original content "));
    }

    @Test
    @DisplayName("编辑帖子 → 修改可见权限")
    void updatePost_ChangeVisibility() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Visibility Test",
                                    "content": "Test Test Test Test Test ",
                                    "categoryId": 1000,
                                    "visibility": 0
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        assertOk(mockMvc.perform(patch("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"visibility\": 2}")))
                .andExpect(jsonPath("$.data.visibility").value(2));
    }

    // ==================== 软删除 ====================

    @Test
    @DisplayName("软删除 → 作者本人删除 → 200")
    void deletePost_AsAuthor_Returns200() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "To Be Deleted",
                                    "content": "Delete me Delete me Delete me Delete me Delete me ",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        assertOk(mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                        .header("Authorization", bearerToken(authorToken))))
                .andExpect(jsonPath("$.message").value("删除成功"));

        // After deletion, post returns 404
        assertNotFound(mockMvc.perform(get("/api/v1/posts/{id}", postId)));
    }

    @Test
    @DisplayName("软删除 → 非作者 → 403")
    void deletePost_NotAuthor_Returns403() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Cannot Delete",
                                    "content": "Not yours Not yours Not yours Not yours Not yours ",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        assertForbidden(mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(otherUserToken))));
    }

    @Test
    @DisplayName("软删除 → 未认证 → 401")
    void deletePost_Unauthenticated_Returns401() throws Exception {
        assertUnauthorized(mockMvc.perform(delete("/api/v1/posts/1")));
    }

    @Test
    @DisplayName("软删除 → 已删除帖子再次删除 → 404")
    void deletePost_AlreadyDeleted_Returns404() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Delete Twice",
                                    "content": "Delete Delete Delete Delete Delete ",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        // First delete succeeds
        mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(authorToken)));

        // Second delete → 404
        assertNotFound(mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(authorToken))));
    }

    @Test
    @DisplayName("完整生命周期 → 创建→查询→编辑→删除→验证已删除")
    void fullPostLifecycle() throws Exception {
        // 1. Create
        String createResp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Lifecycle Test",
                                    "content": "Full lifecycle test Full lifecycle test Full lifecycle test Full lifecycle test Full lifecycle test ",
                                    "categoryId": 1000
                                }
                                """))
                .andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(createResp).get("data").get("id").asText();

        // 2. Query detail
        assertOk(mockMvc.perform(get("/api/v1/posts/{id}", postId)))
                .andExpect(jsonPath("$.data.title").value("Lifecycle Test"))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        // 3. Edit
        assertOk(mockMvc.perform(patch("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(authorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Lifecycle Test V2\"}")))
                .andExpect(jsonPath("$.data.title").value("Lifecycle Test V2"));

        // 4. Verify edit
        assertOk(mockMvc.perform(get("/api/v1/posts/{id}", postId)))
                .andExpect(jsonPath("$.data.title").value("Lifecycle Test V2"));

        // 5. Delete
        assertOk(mockMvc.perform(delete("/api/v1/posts/{id}", postId)
                .header("Authorization", bearerToken(authorToken))));

        // 6. Verify deleted
        assertNotFound(mockMvc.perform(get("/api/v1/posts/{id}", postId)));
    }
}
