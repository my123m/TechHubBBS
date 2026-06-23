package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.comment.CommentCreateRequest;
import com.techhub.dto.comment.CommentVO;
import com.techhub.entity.Comment;
import com.techhub.entity.Post;
import com.techhub.entity.User;
import com.techhub.enums.PostStatusEnum;
import com.techhub.mapper.CommentMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserLikeMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.CommentService;
import com.techhub.service.NotificationService;
import com.techhub.service.PostVisibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final UserLikeMapper userLikeMapper;
    private final PostVisibilityService postVisibilityService;
    private final NotificationService notificationService;

    @Override
    public PageResult<CommentVO> listByPost(Long postId, int page, int size, Long currentUserId) {
        if (size > 50) {
            size = 50;
        }

        // 验证帖子存在且可见
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        boolean isAdmin = isAdmin();
        postVisibilityService.checkVisibleOrThrow(post, currentUserId, isAdmin);

        // 分页查询评论，按创建时间升序
        Page<Comment> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByAsc(Comment::getCreateTime);
        Page<Comment> result = commentMapper.selectPage(mpPage, wrapper);

        List<CommentVO> voList = new ArrayList<>();
        for (Comment comment : result.getRecords()) {
            voList.add(toCommentVO(comment, currentUserId));
        }

        return new PageResult<>(voList, result.getTotal(), size, page);
    }

    @Override
    @Transactional
    public CommentVO create(Long postId, CommentCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        // 验证帖子存在且可见
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        boolean isAdmin = isAdmin();
        postVisibilityService.checkVisibleOrThrow(post, userId, isAdmin);

        // 检查帖子是否被锁定
        if (post.getStatus() != null && post.getStatus().equals(PostStatusEnum.LOCKED.getCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "帖子已被锁定，无法评论");
        }

        // 插入评论
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setLikeCount(0);
        comment.setRecommendCount(0);
        comment.setIsDivine(0);
        commentMapper.insert(comment);

        // 帖子评论数 +1（原子更新，避免读-改-写竞态条件）
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count + 1"));

        // 通知帖子作者（评论者不是帖子作者本人时）
        try {
            if (!userId.equals(post.getAuthorId())) {
                notificationService.create(post.getAuthorId(), "REPLY", comment.getId(), "COMMENT", postId, "回复了你的帖子");
            }
        } catch (Exception e) {
            log.warn("创建回复通知失败: postId={}, commentId={}, error={}", postId, comment.getId(), e.getMessage());
        }

        return toCommentVO(comment, userId);
    }

    @Override
    @Transactional
    public void delete(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }

        // 验证权限：作者本人、管理员或版主可删除
        if (!userId.equals(comment.getUserId()) && !isAdminOrModerator()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除此评论");
        }

        commentMapper.deleteById(commentId);

        // 帖子评论数 -1（原子更新，避免读-改-写竞态条件）
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, comment.getPostId())
                .gt(Post::getCommentCount, 0)
                .setSql("comment_count = comment_count - 1"));

        // 若被删评论为神评，同步递减神评计数（原子更新）
        if (comment.getIsDivine() != null && comment.getIsDivine() == 1) {
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, comment.getPostId())
                    .gt(Post::getDivineCommentCount, 0)
                    .setSql("divine_comment_count = divine_comment_count - 1"));
        }
    }

    // --- 私有辅助方法 -------------------------------------------------------

    /**
     * 将 Comment 实体映射为 CommentVO，包含用户名/头像和当前用户点赞状态。
     */
    private CommentVO toCommentVO(Comment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId().toString());
        vo.setContent(comment.getContent());
        vo.setPostId(comment.getPostId().toString());
        vo.setUserId(comment.getUserId().toString());
        vo.setParentId(comment.getParentId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setRecommendCount(comment.getRecommendCount());
        vo.setIsDivine(comment.getIsDivine() != null && comment.getIsDivine() == 1);
        vo.setDivineTime(comment.getDivineTime());
        vo.setCreateTime(comment.getCreateTime());

        // 作者信息
        User author = userMapper.selectById(comment.getUserId());
        if (author != null) {
            vo.setUsername(author.getUsername());
            vo.setAvatarUrl(author.getAvatarUrl());
        }

        // 当前用户是否已点赞该评论
        if (currentUserId != null) {
            vo.setLiked(userLikeMapper.selectCount(new LambdaQueryWrapper<com.techhub.entity.UserLike>()
                    .eq(com.techhub.entity.UserLike::getUserId, currentUserId)
                    .eq(com.techhub.entity.UserLike::getTargetType, "COMMENT")
                    .eq(com.techhub.entity.UserLike::getTargetId, comment.getId())) > 0);
        }

        return vo;
    }

    /**
     * 判断当前登录用户是否为管理员。
     */
    private boolean isAdmin() {
        String role = SecurityUtils.getCurrentRole();
        return "ADMIN".equals(role);
    }

    /**
     * 判断当前登录用户是否为管理员或版主。
     */
    private boolean isAdminOrModerator() {
        String role = SecurityUtils.getCurrentRole();
        return "ADMIN".equals(role) || "MODERATOR".equals(role);
    }
}
