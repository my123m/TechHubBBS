package com.techhub.service.impl;

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
import com.techhub.service.PostService;
import com.techhub.service.PostVisibilityService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 帖子服务实现 — 帖子 CRUD 与可见性过滤。
 */
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final UserLikeMapper userLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final PostDraftMapper postDraftMapper;
    private final PostVisibilityService postVisibilityService;

    public PostServiceImpl(PostMapper postMapper,
                           UserMapper userMapper,
                           CategoryMapper categoryMapper,
                           UserLikeMapper userLikeMapper,
                           FavoriteMapper favoriteMapper,
                           PostDraftMapper postDraftMapper,
                           PostVisibilityService postVisibilityService) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.userLikeMapper = userLikeMapper;
        this.favoriteMapper = favoriteMapper;
        this.postDraftMapper = postDraftMapper;
        this.postVisibilityService = postVisibilityService;
    }

    @Override
    public PageResult<PostVO> listPosts(PostListQuery query, Long currentUserId) {
        int page = query.getPage() != null ? query.getPage() : 1;
        int size = query.getSize() != null ? query.getSize() : 20;
        if (size > 50) {
            size = 50;
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        // 版块筛选
        if (query.getCategoryId() != null) {
            wrapper.eq(Post::getCategoryId, query.getCategoryId());
        }

        // 关键词搜索（标题 OR 内容）
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(Post::getTitle, query.getKeyword())
                    .or()
                    .like(Post::getContent, query.getKeyword()));
        }

        // 排序
        if ("hot".equals(query.getSort())) {
            wrapper.orderByDesc(Post::getLikeCount);
        } else {
            wrapper.orderByDesc(Post::getType).orderByDesc(Post::getCreateTime);
        }

        boolean isLoggedIn = currentUserId != null;
        boolean isAdmin = isAdmin();
        postVisibilityService.applyVisibilityFilter(wrapper, currentUserId, isLoggedIn, isAdmin);

        Page<Post> mpPage = new Page<>(page, size);
        Page<Post> result = postMapper.selectPage(mpPage, wrapper);

        List<PostVO> voList = result.getRecords().stream()
                .map(p -> toPostVO(p, currentUserId))
                .toList();

        return new PageResult<>(voList, result.getTotal(), size, page);
    }

    @Override
    public PostVO createPost(PostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        // 验证版块存在
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "版块不存在");
        }

        // 构建帖子实体
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategoryId(request.getCategoryId());
        post.setAuthorId(userId);
        post.setVisibility(request.getVisibility() != null ? request.getVisibility() : VisibilityEnum.PUBLIC.getCode());
        post.setType(PostTypeEnum.NORMAL.getCode());
        post.setStatus(PostStatusEnum.NORMAL.getCode());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setDivineCommentCount(0);

        postMapper.insert(post);

        // 如果有关联草稿，发布后删除草稿（仅删除本人的草稿）
        if (request.getDraftPostId() != null) {
            PostDraft draft = postDraftMapper.selectById(request.getDraftPostId());
            if (draft != null && draft.getUserId().equals(userId)) {
                postDraftMapper.deleteById(request.getDraftPostId());
            }
        }

        return toPostVO(post, userId);
    }

    @Override
    public PostVO getPostDetail(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        boolean isAdmin = isAdmin();
        postVisibilityService.checkVisibleOrThrow(post, currentUserId, isAdmin);

        // 浏览量 +1（原子更新，避免读-改-写竞态条件导致漏计数）
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() + 1);

        return toPostVO(post, currentUserId);
    }

    @Override
    public PostVO updatePost(Long postId, PostUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        boolean isAdmin = isAdmin();
        if (!userId.equals(post.getAuthorId()) && !isAdmin) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权修改他人帖子");
        }

        // PATCH 语义：仅更新非 null 字段
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }

        postMapper.updateById(post);

        return toPostVO(post, userId);
    }

    @Override
    public void deletePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        boolean isAdmin = isAdmin();
        if (!userId.equals(post.getAuthorId()) && !isAdmin) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除他人帖子");
        }

        if (postMapper.deleteById(postId) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在或已被删除");
        }
    }

    // --- 私有辅助方法 -------------------------------------------------------

    /**
     * 将 Post 实体映射为 PostVO，包含关联查询（作者名、版块名、点赞/收藏状态）。
     */
    private PostVO toPostVO(Post post, Long currentUserId) {
        PostVO vo = new PostVO();
        vo.setId(post.getId().toString());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCategoryId(post.getCategoryId().toString());
        vo.setAuthorId(post.getAuthorId().toString());
        vo.setType(post.getType());
        vo.setStatus(post.getStatus());
        vo.setVisibility(post.getVisibility());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setDivineCommentCount(post.getDivineCommentCount());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());

        // 作者信息
        User author = userMapper.selectById(post.getAuthorId());
        if (author != null) {
            vo.setAuthorName(author.getUsername());
            vo.setAuthorAvatar(author.getAvatarUrl());
        }

        // 版块名称
        Category cat = categoryMapper.selectById(post.getCategoryId());
        if (cat != null) {
            vo.setCategoryName(cat.getName());
        }

        // 当前用户的点赞/收藏状态
        if (currentUserId != null) {
            vo.setLiked(userLikeMapper.selectCount(new LambdaQueryWrapper<UserLike>()
                    .eq(UserLike::getUserId, currentUserId)
                    .eq(UserLike::getTargetType, "POST")
                    .eq(UserLike::getTargetId, post.getId())) > 0);
            vo.setFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, currentUserId)
                    .eq(Favorite::getPostId, post.getId())) > 0);
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
}
