package com.techhub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.entity.Follow;
import com.techhub.entity.Post;
import com.techhub.enums.VisibilityEnum;
import com.techhub.mapper.FollowMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 帖子可见性服务 — 核心安全模块。
 * 根据帖子可见级别、当前用户身份、关注关系判断帖子是否可见。
 * 不可见帖子一律返回 false / 抛出 NOT_FOUND，防止信息泄露。
 */
@Service
public class PostVisibilityService {

    private final FollowMapper followMapper;

    public PostVisibilityService(FollowMapper followMapper) {
        this.followMapper = followMapper;
    }

    /**
     * 将可见性过滤条件注入查询 wrapper，使分页 total 与可见 records 一致。
     * 语义必须与 isVisible() 保持严格一致；变更本方法时必须同步更新 isVisible()。
     * {@code @TableLogic} 已自动排除 deleted=1 的帖子，此处不再重复处理。
     *
     * @param wrapper       查询条件包装器
     * @param currentUserId 当前登录用户 ID，未登录时为 null
     * @param isLoggedIn    是否已登录
     * @param isAdmin       是否为管理员
     */
    public void applyVisibilityFilter(LambdaQueryWrapper<Post> wrapper,
                                      Long currentUserId, boolean isLoggedIn, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (!isLoggedIn) {
            wrapper.and(w -> w.eq(Post::getVisibility, VisibilityEnum.PUBLIC.getCode()));
            return;
        }
        List<Long> followedIds = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, currentUserId)
        ).stream().map(Follow::getFolloweeId).collect(Collectors.toList());

        wrapper.and(w -> {
            w.eq(Post::getAuthorId, currentUserId)
             .or().eq(Post::getVisibility, VisibilityEnum.PUBLIC.getCode())
             .or().eq(Post::getVisibility, VisibilityEnum.LOGIN_ONLY.getCode());
            if (!followedIds.isEmpty()) {
                w.or(sub -> sub.eq(Post::getVisibility, VisibilityEnum.FOLLOWERS_ONLY.getCode())
                               .in(Post::getAuthorId, followedIds));
            }
        });
    }

    /**
     * 判断帖子是否对当前用户可见。
     *
     * @param post          帖子实体
     * @param currentUserId 当前登录用户 ID，未登录时为 null
     * @param isLoggedIn    是否已登录
     * @param isAdmin       是否为管理员
     * @return 是否可见
     */
    public boolean isVisible(Post post, Long currentUserId, boolean isLoggedIn, boolean isAdmin) {
        // 安全：null 或已删除的帖子不可见，管理员也不能看已删除帖子
        if (post == null || post.getDeleted() != null && post.getDeleted() == 1) {
            return false;
        }

        // 管理员可看所有未删除帖子
        if (isAdmin) {
            return true;
        }

        // 作者可看自己的帖子
        if (currentUserId != null && currentUserId.equals(post.getAuthorId())) {
            return true;
        }

        int visibility = post.getVisibility();

        // 公开 — 所有人可见
        if (visibility == VisibilityEnum.PUBLIC.getCode()) {
            return true;
        }

        // 登录可见 — 仅登录用户可见
        if (visibility == VisibilityEnum.LOGIN_ONLY.getCode()) {
            return isLoggedIn;
        }

        // 粉丝可见 — 登录且关注了作者
        if (visibility == VisibilityEnum.FOLLOWERS_ONLY.getCode()) {
            return currentUserId != null && isFollowing(currentUserId, post.getAuthorId());
        }

        // 私密 — 只有作者和管理员可见（已在上方处理）
        return false;
    }

    /**
     * 检查帖子可见性，不可见时抛出 NOT_FOUND 异常（防止信息泄露）。
     *
     * @param post          帖子实体
     * @param currentUserId 当前登录用户 ID，未登录时为 null
     * @param isAdmin       是否为管理员
     * @throws BusinessException 帖子不可见时抛出，code=404
     */
    public void checkVisibleOrThrow(Post post, Long currentUserId, boolean isAdmin) {
        if (!isVisible(post, currentUserId, currentUserId != null, isAdmin)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
    }

    /**
     * 判断用户是否关注了另一个用户。
     */
    private boolean isFollowing(Long followerId, Long followeeId) {
        return followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, followerId)
                        .eq(Follow::getFolloweeId, followeeId)
        ) > 0;
    }
}
