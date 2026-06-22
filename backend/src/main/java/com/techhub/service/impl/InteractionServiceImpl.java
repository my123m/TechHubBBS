package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.entity.Comment;
import com.techhub.entity.Favorite;

import com.techhub.entity.Post;
import com.techhub.entity.UserLike;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.FavoriteMapper;

import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserLikeMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.DivineCommentService;
import com.techhub.service.InteractionService;
import com.techhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final UserLikeMapper userLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final DivineCommentService divineCommentService;
    private final NotificationService notificationService;

    // ==================== 帖子点赞 ====================

    @Override
    @Transactional
    public void likePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        UserLike userLike = new UserLike();
        userLike.setUserId(userId);
        userLike.setTargetType("POST");
        userLike.setTargetId(postId);
        try {
            userLikeMapper.insert(userLike);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "已经点赞过了");
        }

        post.setLikeCount(post.getLikeCount() + 1);
        postMapper.updateById(post);

        try {
            if (!userId.equals(post.getAuthorId())) {
                notificationService.create(post.getAuthorId(), "LIKE", postId, "POST", null, "赞了你的帖子");
            }
        } catch (Exception e) {
            log.warn("创建帖子点赞通知失败: postId={}, error={}", postId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        LambdaQueryWrapper<UserLike> wrapper = new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, "POST")
                .eq(UserLike::getTargetId, postId);
        int deleted = userLikeMapper.delete(wrapper);

        if (deleted > 0) {
            Post post = postMapper.selectById(postId);
            if (post != null && post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
                postMapper.updateById(post);
            }
        }
        // 幂等：不存在时不抛异常
    }

    // ==================== 帖子收藏 ====================

    @Override
    @Transactional
    public void favoritePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "已经收藏过了");
        }
    }

    @Override
    @Transactional
    public void unfavoritePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getPostId, postId);
        favoriteMapper.delete(wrapper);
        // 幂等：不存在时不抛异常
    }

    // ==================== 评论点赞 ====================

    @Override
    @Transactional
    public void likeComment(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        UserLike userLike = new UserLike();
        userLike.setUserId(userId);
        userLike.setTargetType("COMMENT");
        userLike.setTargetId(commentId);
        try {
            userLikeMapper.insert(userLike);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "已经点赞过了");
        }

        comment.setLikeCount(comment.getLikeCount() + 1);
        divineCommentService.checkAndUpdateDivineStatus(comment);
        commentMapper.updateById(comment);

        try {
            if (!userId.equals(comment.getUserId())) {
                notificationService.create(comment.getUserId(), "LIKE", commentId, "COMMENT", comment.getPostId(), "赞了你的评论");
            }
        } catch (Exception e) {
            log.warn("创建评论点赞通知失败: commentId={}, error={}", commentId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        LambdaQueryWrapper<UserLike> wrapper = new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, "COMMENT")
                .eq(UserLike::getTargetId, commentId);
        int deleted = userLikeMapper.delete(wrapper);

        if (deleted > 0) {
            Comment comment = commentMapper.selectById(commentId);
            if (comment != null && comment.getLikeCount() > 0) {
                comment.setLikeCount(comment.getLikeCount() - 1);
                divineCommentService.checkAndUpdateDivineStatus(comment);
                commentMapper.updateById(comment);
            }
        }
        // 幂等：不存在时不抛异常
    }
}
