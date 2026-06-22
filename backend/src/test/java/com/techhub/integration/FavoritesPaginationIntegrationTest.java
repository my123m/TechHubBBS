package com.techhub.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 收藏分页集成测试 — 验证 getFavorites 分页总数正确排除已删除帖子。
 * 用户 5001 收藏了 15 篇帖子，其中 5 篇已删除（deleted=1），
 * 分页元数据应仅基于 10 篇未删除帖子计算。
 */
@Sql(scripts = "classpath:sql/h2-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = {"classpath:sql/h2-integration-data.sql", "classpath:sql/h2-favorites-pagination-data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("收藏分页集成测试")
@Transactional
class FavoritesPaginationIntegrationTest extends BaseIntegrationTest {

    private static final long FAV_USER_ID = 5001L;

    private String favToken() {
        return bearerToken(generateToken(FAV_USER_ID, "favuser", "USER"));
    }

    @Test
    @DisplayName("size=10,page=1 → total=10,pages=1,records=10（仅未删除）")
    void page1Size10_ReturnsCorrectTotalAndRecords() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .header("Authorization", favToken())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.records", hasSize(10)));
    }

    @Test
    @DisplayName("size=5,page=1 → total=10,pages=2,records=5")
    void page1Size5_ReturnsFiveRecords() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .header("Authorization", favToken())
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.records", hasSize(5)));
    }

    @Test
    @DisplayName("size=5,page=2 → total=10,pages=2,records=5（不出现空页）")
    void page2Size5_ReturnsRemainingRecords() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/favorites")
                        .header("Authorization", favToken())
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.pages").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.records", hasSize(5)));
    }
}
