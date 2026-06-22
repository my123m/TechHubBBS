package com.techhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostCreateRequest;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostUpdateRequest;
import com.techhub.dto.post.PostVO;
import com.techhub.entity.*;
import com.techhub.enums.PostStatusEnum;
import com.techhub.enums.PostTypeEnum;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.*;
import com.techhub.security.SecurityUtils;
import com.techhub.service.impl.PostServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl")
class PostServiceTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private PostDraftMapper postDraftMapper;
    @Mock
    private PostVisibilityService postVisibilityService;

    @InjectMocks
    private PostServiceImpl postService;

    private MockedStatic<SecurityUtils> securityUtils;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long POST_ID = 100L;
    private static final String USERNAME = "testuser";
    private static final String CATEGORY_NAME = "技术讨论";

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    // --- 辅助方法 -----------------------------------------------------------

    private Post createPost(Long id, Long authorId, int visibility) {
        return createPost(id, authorId, visibility, "Test Title", "Test Content", PostTypeEnum.NORMAL.getCode());
    }

    private Post createPost(Long id, Long authorId, int visibility, String title, String content, int type) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(title);
        post.setContent(content);
        post.setCategoryId(CATEGORY_ID);
        post.setAuthorId(authorId);
        post.setType(type);
        post.setStatus(PostStatusEnum.NORMAL.getCode());
        post.setVisibility(visibility);
        post.setViewCount(10);
        post.setLikeCount(5);
        post.setCommentCount(3);
        post.setDivineCommentCount(0);
        post.setDeleted(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        return post;
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setAvatarUrl("https://example.com/avatar.png");
        return user;
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private void mockMapperRelations() {
        lenient().when(userMapper.selectById(anyLong()))
                .thenAnswer(inv -> createUser(inv.getArgument(0), "user" + inv.getArgument(0)));
        lenient().when(categoryMapper.selectById(anyLong()))
                .thenAnswer(inv -> createCategory(inv.getArgument(0), "板块" + inv.getArgument(0)));
        lenient().when(userLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        lenient().when(favoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    }

    // =========================================================================
    // listPosts
    // =========================================================================

    @Nested
    @DisplayName("listPosts")
    class ListPostsTests {

        @Test
        @DisplayName("returns visible posts for authenticated user")
        void returnsVisiblePosts() {
            PostListQuery query = new PostListQuery();
            query.setPage(1);
            query.setSize(10);

            Post post = createPost(POST_ID, USER_ID, VisibilityEnum.PUBLIC.getCode());
            Page<Post> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(post));
            mpPage.setTotal(1);

            when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);
            mockMapperRelations();

            PageResult<PostVO> result = postService.listPosts(query, USER_ID);

            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(POST_ID.toString(), result.getRecords().get(0).getId());
        }

        @Test
        @DisplayName("total 和 records 数量一致 — 回归 issue #14 分页不准确")
        void totalMatchesRecords() {
            PostListQuery query = new PostListQuery();
            query.setPage(1);
            query.setSize(10);

            Post p1 = createPost(100L, USER_ID, VisibilityEnum.PUBLIC.getCode());
            Post p2 = createPost(101L, OTHER_USER_ID, VisibilityEnum.PUBLIC.getCode());
            Page<Post> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(p1, p2));
            mpPage.setTotal(2);

            when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);
            mockMapperRelations();

            PageResult<PostVO> result = postService.listPosts(query, USER_ID);

            assertEquals(2, result.getRecords().size());
            assertEquals(2, result.getTotal());
        }

        @Test
        @DisplayName("returns empty list when no posts match")
        void returnsEmptyList() {
            PostListQuery query = new PostListQuery();
            query.setPage(1);
            query.setSize(10);

            Page<Post> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of());
            mpPage.setTotal(0);

            when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            PageResult<PostVO> result = postService.listPosts(query, USER_ID);

            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("filters by category")
        void filtersByCategory() {
            PostListQuery query = new PostListQuery();
            query.setPage(1);
            query.setSize(10);
            query.setCategoryId(CATEGORY_ID);

            Page<Post> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(createPost(POST_ID, USER_ID, VisibilityEnum.PUBLIC.getCode())));
            mpPage.setTotal(1);

            when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);
            mockMapperRelations();

            PageResult<PostVO> result = postService.listPosts(query, USER_ID);

            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("filters null page/size → uses defaults (page=1, size=20)")
        void defaultsPageSize() {
            PostListQuery query = new PostListQuery();

            Page<Post> mpPage = new Page<>(1, 20);
            mpPage.setRecords(List.of());
            mpPage.setTotal(0);

            when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            postService.listPosts(query, USER_ID);

            verify(postMapper).selectPage(argThat(p -> p.getCurrent() == 1 && p.getSize() == 20),
                    any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("caps size at 50")
        void capsSizeAt50() {
            PostListQuery query = new PostListQuery();
            query.setPage(1);
            query.setSize(100);

            Page<Post> mpPage = new Page<>(1, 50);
            mpPage.setRecords(List.of());
            mpPage.setTotal(0);

            when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            postService.listPosts(query, USER_ID);

            verify(postMapper).selectPage(argThat(p -> p.getSize() == 50),
                    any(LambdaQueryWrapper.class));
        }
    }

    // =========================================================================
    // getPostDetail
    // =========================================================================

    @Nested
    @DisplayName("getPostDetail")
    class GetPostDetailTests {

        @Test
        @DisplayName("returns post detail and increments view count")
        void returnsPostDetailAndIncrementsViews() {
            Post post = createPost(POST_ID, USER_ID, VisibilityEnum.PUBLIC.getCode());
            int originalViews = post.getViewCount();

            when(postMapper.selectById(POST_ID)).thenReturn(post);
            doNothing().when(postVisibilityService).checkVisibleOrThrow(any(Post.class), any(), anyBoolean());
            when(postMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            mockMapperRelations();

            PostVO result = postService.getPostDetail(POST_ID, USER_ID);

            assertNotNull(result);
            assertEquals(POST_ID.toString(), result.getId());
            assertEquals(originalViews + 1, post.getViewCount());
            verify(postMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("throws NOT_FOUND when post does not exist")
        void throwsNotFoundWhenPostMissing() {
            when(postMapper.selectById(POST_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.getPostDetail(POST_ID, USER_ID));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("throws NOT_FOUND when post is not visible")
        void throwsNotFoundWhenNotVisible() {
            Post post = createPost(POST_ID, USER_ID, VisibilityEnum.PRIVATE.getCode());

            when(postMapper.selectById(POST_ID)).thenReturn(post);
            doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                    .when(postVisibilityService).checkVisibleOrThrow(any(Post.class), any(), anyBoolean());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.getPostDetail(POST_ID, OTHER_USER_ID));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // =========================================================================
    // updatePost
    // =========================================================================

    @Nested
    @DisplayName("updatePost")
    class UpdatePostTests {

        @BeforeEach
        void setUp() {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        }

        @Test
        @DisplayName("updates post successfully as author")
        void updatesPostAsAuthor() {
            Post post = createPost(POST_ID, USER_ID, VisibilityEnum.PUBLIC.getCode(),
                    "Old Title", "Old Content", PostTypeEnum.NORMAL.getCode());
            PostUpdateRequest request = new PostUpdateRequest();
            request.setTitle("New Title");
            request.setContent("New Content");

            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);
            mockMapperRelations();

            PostVO result = postService.updatePost(POST_ID, request);

            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals("New Content", result.getContent());
            verify(postMapper).updateById(post);
        }

        @Test
        @DisplayName("only updates non-null fields (PATCH semantics)")
        void patchSemanticsOnlyUpdatesNonNull() {
            Post post = createPost(POST_ID, USER_ID, VisibilityEnum.PUBLIC.getCode(),
                    "Old Title", "Old Content", PostTypeEnum.NORMAL.getCode());
            PostUpdateRequest request = new PostUpdateRequest();
            request.setTitle("New Title");

            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);
            mockMapperRelations();

            PostVO result = postService.updatePost(POST_ID, request);

            assertEquals("New Title", result.getTitle());
            assertEquals("Old Content", result.getContent());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when not logged in")
        void throwsUnauthorizedWhenNotLoggedIn() {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(null);

            PostUpdateRequest request = new PostUpdateRequest();
            request.setTitle("New Title");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.updatePost(POST_ID, request));
            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("throws NOT_FOUND when post does not exist")
        void throwsNotFoundWhenPostMissing() {
            when(postMapper.selectById(POST_ID)).thenReturn(null);

            PostUpdateRequest request = new PostUpdateRequest();
            request.setTitle("New Title");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.updatePost(POST_ID, request));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("throws FORBIDDEN when not author or admin")
        void throwsForbiddenWhenNotAuthorOrAdmin() {
            Post post = createPost(POST_ID, OTHER_USER_ID, VisibilityEnum.PUBLIC.getCode());

            when(postMapper.selectById(POST_ID)).thenReturn(post);

            PostUpdateRequest request = new PostUpdateRequest();
            request.setTitle("New Title");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.updatePost(POST_ID, request));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("allows admin to update any post")
        void allowsAdminToUpdateAnyPost() {
            securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("ADMIN");

            Post post = createPost(POST_ID, OTHER_USER_ID, VisibilityEnum.PUBLIC.getCode());
            PostUpdateRequest request = new PostUpdateRequest();
            request.setTitle("Admin Edit");

            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);
            mockMapperRelations();

            PostVO result = postService.updatePost(POST_ID, request);

            assertNotNull(result);
            assertEquals("Admin Edit", result.getTitle());
        }
    }

    // =========================================================================
    // deletePost
    // =========================================================================

    @Nested
    @DisplayName("deletePost")
    class DeletePostTests {

        @BeforeEach
        void setUp() {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        }

        @Test
        @DisplayName("soft-deletes post as author")
        void softDeletesPostAsAuthor() {
            Post post = createPost(POST_ID, USER_ID, VisibilityEnum.PUBLIC.getCode());

            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.deleteById(POST_ID)).thenReturn(1);

            postService.deletePost(POST_ID);

            verify(postMapper).deleteById(POST_ID);
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when not logged in")
        void throwsUnauthorizedWhenNotLoggedIn() {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.deletePost(POST_ID));
            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("throws NOT_FOUND when post does not exist")
        void throwsNotFoundWhenPostMissing() {
            when(postMapper.selectById(POST_ID)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.deletePost(POST_ID));
            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("throws FORBIDDEN when not author or admin")
        void throwsForbiddenWhenNotAuthorOrAdmin() {
            Post post = createPost(POST_ID, OTHER_USER_ID, VisibilityEnum.PUBLIC.getCode());

            when(postMapper.selectById(POST_ID)).thenReturn(post);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> postService.deletePost(POST_ID));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("allows admin to delete any post")
        void allowsAdminToDeleteAnyPost() {
            securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("ADMIN");

            Post post = createPost(POST_ID, OTHER_USER_ID, VisibilityEnum.PUBLIC.getCode());

            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.deleteById(POST_ID)).thenReturn(1);

            postService.deletePost(POST_ID);

            verify(postMapper).deleteById(POST_ID);
        }
    }
}
