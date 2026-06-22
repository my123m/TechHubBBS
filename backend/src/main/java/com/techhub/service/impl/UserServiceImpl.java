package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.common.BusinessException;
import com.techhub.common.PageResult;
import com.techhub.common.ResultCode;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostVO;
import com.techhub.dto.user.UserDetailVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.dto.user.UserUpdateRequest;
import com.techhub.entity.Favorite;
import com.techhub.entity.Post;
import com.techhub.entity.User;
import com.techhub.mapper.FavoriteMapper;
import com.techhub.mapper.PostMapper;
import com.techhub.mapper.UserMapper;
import com.techhub.security.SecurityUtils;
import com.techhub.service.PostVisibilityService;
import com.techhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final FavoriteMapper favoriteMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PostVisibilityService postVisibilityService;

    @Override
    public UserDetailVO getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        return toUserDetailVO(user);
    }

    @Override
    @Transactional
    public void updateProfile(UserUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = new User();
        user.setId(userId);
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        userMapper.updateById(user);
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toUserProfileVO(user);
    }

    @Override
    public PageResult<PostVO> getUserPosts(Long userId, PostListQuery query, Long currentUserId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getAuthorId, userId)
                .orderByDesc(Post::getCreateTime);

        boolean isLoggedIn = currentUserId != null;
        boolean isAdmin = isAdmin();
        postVisibilityService.applyVisibilityFilter(wrapper, currentUserId, isLoggedIn, isAdmin);

        Page<Post> page = new Page<>(query.getPage(), query.getSize());
        Page<Post> resultPage = postMapper.selectPage(page, wrapper);

        List<PostVO> records = resultPage.getRecords().stream()
                .map(this::toPostVO)
                .toList();
        return PageResult.of(records, resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        if (oldPwd.equals(newPwd)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(update);
    }

    @Override
    public PageResult<PostVO> getFavorites(Long userId, int page, int size) {
        Page<Favorite> favPage = new Page<>(page, size);
        Page<Favorite> favResult = favoriteMapper.selectVisibleFavorites(favPage, userId);

        if (favResult.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, size, page);
        }

        List<Long> postIds = favResult.getRecords().stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());

        List<Post> posts = postMapper.selectBatchIds(postIds);

        List<PostVO> records = posts.stream()
                .map(this::toPostVO)
                .collect(Collectors.toList());

        return PageResult.of(records, favResult.getTotal(), size, page);
    }

    private boolean isAdmin() {
        String role = SecurityUtils.getCurrentRole();
        return "ADMIN".equals(role);
    }

    // ---- mapping helpers ----

    private UserDetailVO toUserDetailVO(User user) {
        if (user == null) {
            return null;
        }
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId().toString());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

        private UserProfileVO toUserProfileVO(User user) {
            if (user == null) {
                return null;
            }
            UserProfileVO vo = new UserProfileVO();
            vo.setId(user.getId().toString());
            vo.setUsername(user.getUsername());
            vo.setAvatarUrl(user.getAvatarUrl());
            vo.setBio(user.getBio());
            vo.setRole(user.getRole());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            return vo;
        }

    private PostVO toPostVO(Post post) {
        if (post == null) {
            return null;
        }
        PostVO vo = new PostVO();
        vo.setId(post.getId().toString());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCategoryId(post.getCategoryId() != null ? post.getCategoryId().toString() : null);
        vo.setAuthorId(post.getAuthorId() != null ? post.getAuthorId().toString() : null);
        vo.setType(post.getType());
        vo.setStatus(post.getStatus());
        vo.setVisibility(post.getVisibility());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setDivineCommentCount(post.getDivineCommentCount());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        return vo;
    }
}
