package com.techhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.comment.CommentCreateRequest;
import com.techhub.dto.comment.CommentVO;
import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.entity.User;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserLikeMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl")
class CommentServiceImplTest {

    @Mock private CommentMapper commentMapper;
    @Mock private PostMapper postMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserLikeMapper userLikeMapper;
    @Mock private PostVisibilityService postVisibilityService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private MockedStatic<SecurityUtils> securityUtils;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long COMMENT_ID = 100L;
    private static final Long POST_ID = 10L;
    private static final Long AUTHOR_ID = 5L;

    @BeforeEach void setUp() { securityUtils = mockStatic(SecurityUtils.class); }
    @AfterEach void tearDown() { securityUtils.close(); }

    private void mockAuth(Long uid, String role) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(uid);
        securityUtils.when(SecurityUtils::getCurrentRole).thenReturn(role);
    }

    private Comment createComment(Long id, Long postId, Long userId, String content) {
        Comment c = new Comment();
        c.setId(id);
        c.setPostId(postId);
        c.setUserId(userId);
        c.setContent(content);
        c.setLikeCount(0);
        c.setRecommendCount(0);
        c.setIsDivine(0);
        c.setCreateTime(LocalDateTime.now());
        return c;
    }

    private Post createPost(Long id, Long authorId, int commentCount) {
        Post p = new Post();
        p.setId(id);
        p.setAuthorId(authorId);
        p.setTitle("Test Post");
        p.setCommentCount(commentCount);
        return p;
    }

    private User createUser(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("管理员删除他人评论成功")
        void asAdminDeleteOthersSucceeds() {
            mockAuth(USER_ID, "ADMIN");
            Comment comment = createComment(COMMENT_ID, POST_ID, OTHER_USER_ID, "其他用户评论");
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);
            when(commentMapper.deleteById(COMMENT_ID)).thenReturn(1);
            Post post = createPost(POST_ID, AUTHOR_ID, 3);
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            commentService.delete(COMMENT_ID);

            verify(commentMapper).deleteById(COMMENT_ID);
            verify(postMapper).updateById(any(Post.class));
        }

        @Test
        @DisplayName("版主删除他人评论成功")
        void asModeratorDeleteOthersSucceeds() {
            mockAuth(USER_ID, "MODERATOR");
            Comment comment = createComment(COMMENT_ID, POST_ID, OTHER_USER_ID, "其他用户评论");
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);
            when(commentMapper.deleteById(COMMENT_ID)).thenReturn(1);
            Post post = createPost(POST_ID, AUTHOR_ID, 3);
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            commentService.delete(COMMENT_ID);

            verify(commentMapper).deleteById(COMMENT_ID);
        }

        @Test
        @DisplayName("普通用户删除他人评论 -> FORBIDDEN")
        void asUserDeleteOthersForbidden() {
            mockAuth(USER_ID, "USER");
            Comment comment = createComment(COMMENT_ID, POST_ID, OTHER_USER_ID, "其他用户评论");
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> commentService.delete(COMMENT_ID));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
            verify(commentMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("作者删除自己评论成功")
        void asAuthorDeleteOwnSucceeds() {
            mockAuth(USER_ID, "USER");
            Comment comment = createComment(COMMENT_ID, POST_ID, USER_ID, "我的评论");
            when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);
            when(commentMapper.deleteById(COMMENT_ID)).thenReturn(1);
            Post post = createPost(POST_ID, AUTHOR_ID, 3);
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);

            commentService.delete(COMMENT_ID);

            verify(commentMapper).deleteById(COMMENT_ID);
        }
    }

    @Nested
    @DisplayName("listByPost — 可见性传递")
    class ListByPostTests {

        @Test
        @DisplayName("管理员查看 -> isAdmin=true 传给 checkVisibleOrThrow")
        void asAdminPassesIsAdminTrue() {
            mockAuth(USER_ID, "ADMIN");
            Post post = createPost(POST_ID, AUTHOR_ID, 5);
            when(postMapper.selectById(POST_ID)).thenReturn(post);

            Page<Comment> mpPage = new Page<>(1, 20);
            mpPage.setRecords(List.of());
            mpPage.setTotal(0);
            when(commentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            commentService.listByPost(POST_ID, 1, 20, USER_ID);

            verify(postVisibilityService).checkVisibleOrThrow(post, USER_ID, true);
        }

        @Test
        @DisplayName("普通用户查看 -> isAdmin=false 传给 checkVisibleOrThrow")
        void asUserPassesIsAdminFalse() {
            mockAuth(USER_ID, "USER");
            Post post = createPost(POST_ID, AUTHOR_ID, 5);
            when(postMapper.selectById(POST_ID)).thenReturn(post);

            Page<Comment> mpPage = new Page<>(1, 20);
            mpPage.setRecords(List.of());
            mpPage.setTotal(0);
            when(commentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            commentService.listByPost(POST_ID, 1, 20, USER_ID);

            verify(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        }

        @Test
        @DisplayName("size 超过 50 上限 -> 截断为 50")
        void sizeCappedAt50() {
            mockAuth(USER_ID, "USER");
            Post post = createPost(POST_ID, AUTHOR_ID, 5);
            when(postMapper.selectById(POST_ID)).thenReturn(post);

            Page<Comment> mpPage = new Page<>(1, 50);
            mpPage.setRecords(List.of());
            mpPage.setTotal(0);
            when(commentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mpPage);

            commentService.listByPost(POST_ID, 1, 100, USER_ID);

            ArgumentCaptor<Page<Comment>> pageCaptor = ArgumentCaptor.forClass(Page.class);
            verify(commentMapper).selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class));
            assertEquals(50, pageCaptor.getValue().getSize());
        }
    }

    @Nested
    @DisplayName("create — 可见性传递与帖子锁定")
    class CreateTests {

        @Test
        @DisplayName("管理员评论 -> isAdmin=true 传给 checkVisibleOrThrow")
        void asAdminPassesIsAdminTrue() {
            mockAuth(USER_ID, "ADMIN");
            Post post = createPost(POST_ID, AUTHOR_ID, 3);
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            doAnswer(inv -> { inv.getArgument(0, Comment.class).setId(200L); return 1; })
                    .when(commentMapper).insert(any(Comment.class));
            when(postMapper.updateById(any(Post.class))).thenReturn(1);
            when(userMapper.selectById(USER_ID)).thenReturn(createUser(USER_ID, "admin"));

            CommentCreateRequest request = new CommentCreateRequest();
            request.setContent("管理员评论");
            commentService.create(POST_ID, request);

            verify(postVisibilityService).checkVisibleOrThrow(post, USER_ID, true);
        }

        @Test
        @DisplayName("普通用户评论 -> isAdmin=false 传给 checkVisibleOrThrow")
        void asUserPassesIsAdminFalse() {
            mockAuth(USER_ID, "USER");
            Post post = createPost(POST_ID, AUTHOR_ID, 3);
            when(postMapper.selectById(POST_ID)).thenReturn(post);
            doAnswer(inv -> { inv.getArgument(0, Comment.class).setId(200L); return 1; })
                    .when(commentMapper).insert(any(Comment.class));
            when(postMapper.updateById(any(Post.class))).thenReturn(1);
            when(userMapper.selectById(USER_ID)).thenReturn(createUser(USER_ID, "testuser"));

            CommentCreateRequest request = new CommentCreateRequest();
            request.setContent("普通评论");
            commentService.create(POST_ID, request);

            verify(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        }
    }
}
