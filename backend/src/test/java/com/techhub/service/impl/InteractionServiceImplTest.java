package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.entity.UserLike;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.FavoriteMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserLikeMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.DivineCommentService;
import com.techhub.service.NotificationService;
import com.techhub.service.PostVisibilityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InteractionServiceImpl 单元测试")
class InteractionServiceImplTest {

    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private DivineCommentService divineCommentService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PostVisibilityService postVisibilityService;

    @InjectMocks
    private InteractionServiceImpl interactionService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private static final Long USER_ID = 2L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 200L;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

        Post mockPost = new Post();
        mockPost.setId(POST_ID);
        mockPost.setAuthorId(1L);
        mockPost.setLikeCount(0);
        when(postMapper.selectById(POST_ID)).thenReturn(mockPost);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    private void givenCurrentUser(Long userId) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
    }

    // ==================== likePost ====================

    @Test
    @DisplayName("likePost 通知：sourceType=POST, parentId=null")
    void likePostShouldCreateNotificationWithPostSourceType() {
        doReturn(1).when(userLikeMapper).insert(any(UserLike.class));
        when(postMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        interactionService.likePost(POST_ID);

        verify(notificationService).create(eq(1L), eq("LIKE"), eq(POST_ID),
                eq("POST"), isNull(), eq("赞了你的帖子"));
    }

    @Test
    @DisplayName("不同用户点赞同一帖子，每次都产生通知（核心回归）")
    void likePost_DifferentUsers_EachCreatesNotification() {
        Long postId = 1L;
        Long authorId = 2L;
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setLikeCount(0);

        when(postMapper.selectById(postId)).thenReturn(post);
        when(postMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        // 用户 B（id=3）点赞帖子 A（作者=2）
        givenCurrentUser(3L);
        interactionService.likePost(postId);
        verify(notificationService).create(authorId, "LIKE", postId, "POST", null, "赞了你的帖子");

        // 用户 C（id=4）点赞同一帖子
        givenCurrentUser(4L);
        interactionService.likePost(postId);
        // 确认通知被调用了两次（用户 B 和 用户 C 各一次）
        verify(notificationService, times(2)).create(eq(authorId), eq("LIKE"), eq(postId), eq("POST"), isNull(), eq("赞了你的帖子"));
    }

    @Test
    @DisplayName("点赞自己的帖子不产生通知")
    void likePost_SelfLike_NoNotification() {
        Long postId = 1L;
        Long authorId = 2L;
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);
        post.setLikeCount(0);

        when(postMapper.selectById(postId)).thenReturn(post);
        when(postMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        givenCurrentUser(authorId);

        interactionService.likePost(postId);
        verify(notificationService, never()).create(anyLong(), anyString(), anyLong(), any(), any(), anyString());
    }

    @Test
    @DisplayName("帖子不存在时抛出 NOT_FOUND")
    void likePost_PostNotFound_ThrowsNotFound() {
        when(postMapper.selectById(1L)).thenReturn(null);
        givenCurrentUser(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likePost(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("重复点赞抛出 CONFLICT")
    void likePost_DuplicateLike_ThrowsConflict() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setLikeCount(0);

        when(postMapper.selectById(1L)).thenReturn(post);
        when(userLikeMapper.insert(any(UserLike.class))).thenThrow(new DuplicateKeyException("dup"));
        givenCurrentUser(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likePost(1L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("未登录时点赞抛出 UNAUTHORIZED")
    void likePost_Unauthenticated_ThrowsUnauthorized() {
        givenCurrentUser(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likePost(1L));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    // ==================== likeComment ====================

    @Test
    @DisplayName("likeComment 通知：sourceType=COMMENT, parentId=comment.getPostId()")
    void likeCommentShouldCreateNotificationWithCommentSourceType() {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

        Comment mockComment = new Comment();
        mockComment.setId(COMMENT_ID);
        mockComment.setUserId(3L);
        mockComment.setPostId(POST_ID);
        mockComment.setLikeCount(0);
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(mockComment);
        doReturn(1).when(userLikeMapper).insert(any(UserLike.class));
        doNothing().when(divineCommentService).checkAndUpdateDivineStatus(any());
        when(commentMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        interactionService.likeComment(COMMENT_ID);

        verify(notificationService).create(eq(3L), eq("LIKE"), eq(COMMENT_ID),
                eq("COMMENT"), eq(POST_ID), eq("赞了你的评论"));
    }

    @Test
    @DisplayName("不同用户点赞同一评论，每次都产生通知（核心回归）")
    void likeComment_DifferentUsers_EachCreatesNotification() {
        Long commentId = 1L;
        Long commentAuthorId = 2L;
        Long postId = 10L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(commentAuthorId);
        comment.setPostId(postId);
        comment.setLikeCount(0);

        when(commentMapper.selectById(commentId)).thenReturn(comment);
        when(commentMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        Post parentPost = new Post();
        parentPost.setId(postId);
        when(postMapper.selectById(postId)).thenReturn(parentPost);

        // 用户 B（id=3）点赞评论
        givenCurrentUser(3L);
        interactionService.likeComment(commentId);
        verify(notificationService).create(commentAuthorId, "LIKE", commentId, "COMMENT", postId, "赞了你的评论");

        // 用户 C（id=4）点赞同一评论
        givenCurrentUser(4L);
        interactionService.likeComment(commentId);
        verify(notificationService, times(2)).create(eq(commentAuthorId), eq("LIKE"), eq(commentId), eq("COMMENT"), eq(postId), eq("赞了你的评论"));
    }

    @Test
    @DisplayName("点赞自己的评论不产生通知")
    void likeComment_SelfLike_NoNotification() {
        Long commentId = 1L;
        Long authorId = 2L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setUserId(authorId);
        comment.setPostId(10L);
        comment.setLikeCount(0);

        when(commentMapper.selectById(commentId)).thenReturn(comment);
        when(commentMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        Post parentPost = new Post();
        parentPost.setId(10L);
        when(postMapper.selectById(10L)).thenReturn(parentPost);
        givenCurrentUser(authorId);

        interactionService.likeComment(commentId);
        verify(notificationService, never()).create(anyLong(), anyString(), anyLong(), any(), any(), anyString());
    }

    @Test
    @DisplayName("评论不存在时抛出 NOT_FOUND")
    void likeComment_CommentNotFound_ThrowsNotFound() {
        when(commentMapper.selectById(1L)).thenReturn(null);
        givenCurrentUser(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likeComment(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("重复点赞评论抛出 CONFLICT")
    void likeComment_DuplicateLike_ThrowsConflict() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(2L);
        comment.setPostId(10L);
        comment.setLikeCount(0);

        when(commentMapper.selectById(1L)).thenReturn(comment);
        Post parentPost = new Post();
        parentPost.setId(10L);
        when(postMapper.selectById(10L)).thenReturn(parentPost);
        when(userLikeMapper.insert(any(UserLike.class))).thenThrow(new DuplicateKeyException("dup"));
        givenCurrentUser(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likeComment(1L));
        assertEquals(ResultCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("未登录时点赞评论抛出 UNAUTHORIZED")
    void likeComment_Unauthenticated_ThrowsUnauthorized() {
        givenCurrentUser(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likeComment(1L));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    // ==================== notificationService 异常容错 ====================

    @Test
    @DisplayName("通知创建异常时不影响点赞主流程")
    void likePost_NotificationException_DoesNotInterruptLike() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setLikeCount(0);

        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        doThrow(new RuntimeException("通知服务不可用"))
                .when(notificationService).create(anyLong(), anyString(), anyLong(), any(), any(), anyString());
        givenCurrentUser(3L);

        assertDoesNotThrow(() -> interactionService.likePost(1L));
        // 点赞操作应完成（使用原子 SQL 更新）
        verify(postMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ==================== 可见性校验 ====================

    @Test
    @DisplayName("不可见帖子点赞抛出 NOT_FOUND")
    void likePost_PostNotVisible_ThrowsNotFound() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);

        when(postMapper.selectById(1L)).thenReturn(post);
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        givenCurrentUser(USER_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likePost(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("不可见帖子取消点赞抛出 NOT_FOUND")
    void unlikePost_PostNotVisible_ThrowsNotFound() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);

        when(postMapper.selectById(1L)).thenReturn(post);
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        givenCurrentUser(USER_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.unlikePost(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("不可见帖子收藏抛出 NOT_FOUND")
    void favoritePost_PostNotVisible_ThrowsNotFound() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);

        when(postMapper.selectById(1L)).thenReturn(post);
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        givenCurrentUser(USER_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.favoritePost(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("不可见帖子取消收藏抛出 NOT_FOUND")
    void unfavoritePost_PostNotVisible_ThrowsNotFound() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);

        when(postMapper.selectById(1L)).thenReturn(post);
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        givenCurrentUser(USER_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.unfavoritePost(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("点赞不可见帖子的评论抛出 NOT_FOUND")
    void likeComment_ParentPostNotVisible_ThrowsNotFound() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(3L);
        comment.setPostId(10L);
        comment.setLikeCount(0);

        when(commentMapper.selectById(1L)).thenReturn(comment);
        Post post = new Post();
        post.setId(10L);
        when(postMapper.selectById(10L)).thenReturn(post);
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        givenCurrentUser(USER_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.likeComment(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("取消点赞不可见帖子的评论抛出 NOT_FOUND")
    void unlikeComment_ParentPostNotVisible_ThrowsNotFound() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setUserId(3L);
        comment.setPostId(10L);
        comment.setLikeCount(0);

        when(commentMapper.selectById(1L)).thenReturn(comment);
        Post post = new Post();
        post.setId(10L);
        when(postMapper.selectById(10L)).thenReturn(post);
        doThrow(new BusinessException(ResultCode.NOT_FOUND, "帖子不存在"))
                .when(postVisibilityService).checkVisibleOrThrow(post, USER_ID, false);
        givenCurrentUser(USER_ID);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interactionService.unlikeComment(1L));
        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }
}
