package com.techhub.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 可见权限矩阵集成测试：公开/登录可见/粉丝可见/私密 × 游客/普通用户/粉丝/作者/管理员。
 * 验证四级可见权限 + 三级角色权限的交叉校验逻辑。
 */
@Sql(scripts = {"classpath:sql/h2-schema.sql", "classpath:sql/h2-integration-data.sql"})
@Transactional
@DisplayName("可见权限矩阵集成测试")
class VisibilityMatrixIntegrationTest extends BaseIntegrationTest {

    private String authorToken;
    private String followerToken;
    private String strangerToken;
    private String adminToken;

    private String authorId;

    @BeforeEach
    void setUpUsers() throws Exception {
        // Register author
        registerAndLogin("vis_author", "vis_author@test.com");
        String authorResp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vis_author\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        authorToken = objectMapper.readTree(authorResp).get("data").get("token").asText();
        authorId = objectMapper.readTree(authorResp).get("data").get("userId").asText();

        // Register follower — will follow author
        registerAndLogin("vis_follower", "vis_follower@test.com");
        String followerResp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vis_follower\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        followerToken = objectMapper.readTree(followerResp).get("data").get("token").asText();

        // Register stranger — will NOT follow author
        registerAndLogin("vis_stranger", "vis_stranger@test.com");
        String strangerResp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vis_stranger\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        strangerToken = objectMapper.readTree(strangerResp).get("data").get("token").asText();

        // Admin — use generated token with ADMIN role
        adminToken = generateToken(777L, "vis_admin", "ADMIN");

        // Follower follows author
        mockMvc.perform(post("/api/v1/users/{id}/follow", authorId)
                .header("Authorization", bearerToken(followerToken)));
    }

    private void registerAndLogin(String username, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                        {"username":"%s","password":"password123","email":"%s"}
                        """, username, email)));
    }

    private String login(String username) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"password123\"}", username)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("token").asText();
    }

    private String getUserId(String token) throws Exception {
        return jwtTokenProvider.getUserIdFromToken(token).toString();
    }
    private String createPost(int visibility) throws Exception {
        String resp = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "title": "Visibility=%d Post",
                                    "content": "Testing visibility level %d with sufficient content length for testing. Testing visibility level %d with sufficient content length for testing. Testing visibility level %d with sufficient content length for testing. ",
                                    "categoryId": 1000,
                                    "visibility": %d
                                }
                                """, visibility, visibility, visibility, visibility, visibility)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("id").asText();
    }

    // ==================== PUBLIC (0) ====================

    @Nested
    @DisplayName("visibility=PUBLIC(0)")
    class PublicVisibility {

        @Test
        @DisplayName("游客 → 可见")
        void guestCanSee() throws Exception {
            String postId = createPost(0);
            mockMvc.perform(get("/api/v1/posts/{id}", postId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(postId));
        }

        @Test
        @DisplayName("普通用户 → 可见")
        void strangerCanSee() throws Exception {
            String postId = createPost(0);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("粉丝 → 可见")
        void followerCanSee() throws Exception {
            String postId = createPost(0);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(followerToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("作者 → 可见")
        void authorCanSee() throws Exception {
            String postId = createPost(0);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(authorToken)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== LOGIN_ONLY (1) ====================

    @Nested
    @DisplayName("visibility=LOGIN_ONLY(1)")
    class LoginOnlyVisibility {

        @Test
        @DisplayName("游客 → 不可见(404)")
        void guestCannotSee() throws Exception {
            String postId = createPost(1);
            mockMvc.perform(get("/api/v1/posts/{id}", postId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("普通用户 → 可见")
        void strangerCanSee() throws Exception {
            String postId = createPost(1);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("粉丝 → 可见")
        void followerCanSee() throws Exception {
            String postId = createPost(1);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(followerToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("作者 → 可见")
        void authorCanSee() throws Exception {
            String postId = createPost(1);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(authorToken)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== FOLLOWERS_ONLY (2) ====================

    @Nested
    @DisplayName("visibility=FOLLOWERS_ONLY(2)")
    class FollowersOnlyVisibility {

        @Test
        @DisplayName("游客 → 不可见(404)")
        void guestCannotSee() throws Exception {
            String postId = createPost(2);
            mockMvc.perform(get("/api/v1/posts/{id}", postId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("普通用户(非粉丝) → 不可见(404)")
        void strangerCannotSee() throws Exception {
            String postId = createPost(2);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("粉丝 → 可见")
        void followerCanSee() throws Exception {
            String postId = createPost(2);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(followerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(postId));
        }

        @Test
        @DisplayName("作者 → 可见")
        void authorCanSee() throws Exception {
            String postId = createPost(2);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(authorToken)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== PRIVATE (3) ====================

    @Nested
    @DisplayName("visibility=PRIVATE(3)")
    class PrivateVisibility {

        @Test
        @DisplayName("游客 → 不可见(404)")
        void guestCannotSee() throws Exception {
            String postId = createPost(3);
            mockMvc.perform(get("/api/v1/posts/{id}", postId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("普通用户 → 不可见(404)")
        void strangerCannotSee() throws Exception {
            String postId = createPost(3);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("粉丝 → 不可见(404)")
        void followerCannotSee() throws Exception {
            String postId = createPost(3);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(followerToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("作者 → 可见")
        void authorCanSee() throws Exception {
            String postId = createPost(3);
            mockMvc.perform(get("/api/v1/posts/{id}", postId)
                            .header("Authorization", bearerToken(authorToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(postId));
        }
    }

    // ==================== 列表过滤验证 ====================

    @Test
    @DisplayName("帖子列表 → 游客只看到公开帖 → 登录必见/粉丝必见/私密不可见")
    void listPosts_GuestOnlySeesPublic() throws Exception {
        createPost(0); // PUBLIC
        createPost(1); // LOGIN_ONLY
        createPost(2); // FOLLOWERS_ONLY
        createPost(3); // PRIVATE

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].title").value("Visibility=0 Post"));
    }

    @Test
    @DisplayName("帖子列表 → 登录用户看到公开+登录必见 → 粉丝必见过滤非粉丝")
    void listPosts_LoggedInStrangerSeesPublicAndLoginOnly() throws Exception {
        createPost(0); // PUBLIC
        createPost(1); // LOGIN_ONLY
        createPost(2); // FOLLOWERS_ONLY (stranger is not follower)
        createPost(3); // PRIVATE

        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", bearerToken(strangerToken))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(2)));
    }

    @Test
    @DisplayName("帖子列表 → 粉丝看到公开+登录必见+粉丝必见 → 私密不可见")
    void listPosts_FollowerSeesPublicLoginAndFollowersOnly() throws Exception {
        createPost(0); // PUBLIC
        createPost(1); // LOGIN_ONLY
        createPost(2); // FOLLOWERS_ONLY (is follower)
        createPost(3); // PRIVATE

        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", bearerToken(followerToken))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(3)));
    }

    @Test
    @DisplayName("帖子列表 → 作者看到自己所有帖子（含私密）")
    void listPosts_AuthorSeesAllOwnPosts() throws Exception {
        createPost(0);
        createPost(1);
        createPost(2);
        createPost(3);

        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", bearerToken(authorToken))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(4)));
    }

    @Test
    @DisplayName("帖子列表分页 → total 与 pages 仅基于可见帖子（回归 issue #14）")
    void listPosts_PaginationTotalReflectsVisibleOnly() throws Exception {
        // 创建 15 公开 + 10 私密，游客不可见私密
        for (int i = 0; i < 15; i++) {
            createPost(0); // PUBLIC
        }
        for (int i = 0; i < 10; i++) {
            createPost(3); // PRIVATE
        }

        // 游客请求 page 1 size=10 → 应看到 10 条公开帖，total=15，pages=2
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records", hasSize(10)));

        // 游客请求 page 2 size=10 → 应看到剩余 5 条公开帖
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.records", hasSize(5)));
    }

    // ==================== 管理员权限 ====================

    @Test
    @DisplayName("管理员可查看所有权限级别的帖子")
    void adminCanSeeAll() throws Exception {
        String post0 = createPost(0);
        String post1 = createPost(1);
        String post2 = createPost(2);
        String post3 = createPost(3);

        // Admin can see all posts
        mockMvc.perform(get("/api/v1/posts/{id}", post3)
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/{id}", post2)
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk());
    }

    // ==================== 用户帖子列表可见性验证 ====================

    @Nested
    @DisplayName("用户帖子列表 GET /users/{id}/posts")
    class UserPostsListVisibility {

        private String authorId;

        @BeforeEach
        void setUpPosts() throws Exception {
            authorId = getUserId(authorToken);
            createPost(0); // PUBLIC
            createPost(1); // LOGIN_ONLY
            createPost(2); // FOLLOWERS_ONLY
            createPost(3); // PRIVATE
        }

        @Test
        @DisplayName("游客 → 只能看到公开帖(1条)")
        void guestOnlySeesPublic() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records", hasSize(1)))
                    .andExpect(jsonPath("$.data.records[0].title").value("Visibility=0 Post"));
        }

        @Test
        @DisplayName("陌生人 → 看到公开+登录可见(2条)")
        void strangerSeesPublicAndLoginOnly() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                            .header("Authorization", bearerToken(strangerToken))
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records", hasSize(2)));
        }

        @Test
        @DisplayName("粉丝 → 看到公开+登录可见+粉丝可见(3条)")
        void followerSeesPublicLoginAndFollowersOnly() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                            .header("Authorization", bearerToken(followerToken))
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records", hasSize(3)));
        }

        @Test
        @DisplayName("作者 → 看到自己所有帖子含私密(4条)")
        void authorSeesAllOwnPosts() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                            .header("Authorization", bearerToken(authorToken))
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records", hasSize(4)));
        }

        @Test
        @DisplayName("管理员 → 通过权限看到所有帖子(4条)")
        void adminSeesAllPosts() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                            .header("Authorization", bearerToken(adminToken))
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records", hasSize(4)));
        }
    }

    @Test
    @DisplayName("用户帖子列表分页 → total 与 pages 仅基于可见帖子（回归 issue #14，getUserPosts）")
    void getUserPosts_PaginationTotalReflectsVisibleOnly() throws Exception {
        // 创建 15 公开 + 10 私密
        for (int i = 0; i < 15; i++) {
            createPost(0); // PUBLIC
        }
        for (int i = 0; i < 10; i++) {
            createPost(3); // PRIVATE
        }

        // 游客查看作者帖子 → 仅 15 条公开可见，total=15
        mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.records", hasSize(10)));

        // 陌生人查看作者帖子 → 也仅见 15 条（作者不是他的关注对象）
        mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                        .header("Authorization", bearerToken(strangerToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(15));

        // 作者查看自己帖子 → 全部 25 条可见（含私密）
        mockMvc.perform(get("/api/v1/users/{id}/posts", authorId)
                        .header("Authorization", bearerToken(authorToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(25))
                .andExpect(jsonPath("$.data.pages").value(3))
                .andExpect(jsonPath("$.data.records", hasSize(10)));
    }

    // ==================== 交互可见性 ====================

    @Nested
    @DisplayName("InteractionVisibility — like/favorite 可见性校验")
    class InteractionVisibility {

        @Test
        @DisplayName("陌生人点赞私密帖子 → 404")
        void likePrivatePost_Stranger_Returns404() throws Exception {
            String postId = createPost(3); // PRIVATE
            mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("陌生人收藏私密帖子 → 404")
        void favoritePrivatePost_Stranger_Returns404() throws Exception {
            String postId = createPost(3); // PRIVATE
            mockMvc.perform(post("/api/v1/posts/{id}/favorites", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("陌生人点赞登录可见帖子 → 200")
        void likeLoginOnlyPost_Stranger_Returns200() throws Exception {
            String postId = createPost(1); // LOGIN_ONLY
            mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("陌生人收藏登录可见帖子 → 200")
        void favoriteLoginOnlyPost_Stranger_Returns200() throws Exception {
            String postId = createPost(1); // LOGIN_ONLY
            mockMvc.perform(post("/api/v1/posts/{id}/favorites", postId)
                            .header("Authorization", bearerToken(strangerToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("作者点赞自己的私密帖子 → 200")
        void likePrivatePost_Author_Returns200() throws Exception {
            String postId = createPost(3); // PRIVATE
            mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                            .header("Authorization", bearerToken(authorToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("作者收藏自己的私密帖子 → 200")
        void favoritePrivatePost_Author_Returns200() throws Exception {
            String postId = createPost(3); // PRIVATE
            mockMvc.perform(post("/api/v1/posts/{id}/favorites", postId)
                            .header("Authorization", bearerToken(authorToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("管理员点赞私密帖子 → 200")
        void likePrivatePost_Admin_Returns200() throws Exception {
            String postId = createPost(3); // PRIVATE
            mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                            .header("Authorization", bearerToken(adminToken)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("粉丝点赞粉丝可见帖子 → 200")
        void likeFollowersOnlyPost_Follower_Returns200() throws Exception {
            String postId = createPost(2); // FOLLOWERS_ONLY
            mockMvc.perform(post("/api/v1/posts/{id}/likes", postId)
                            .header("Authorization", bearerToken(followerToken)))
                    .andExpect(status().isOk());
        }
    }
}
