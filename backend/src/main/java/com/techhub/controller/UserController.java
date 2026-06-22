package com.techhub.controller;

import com.techhub.common.PageResult;
import com.techhub.common.R;
import com.techhub.dto.auth.PasswordChangeRequest;
import com.techhub.dto.post.PostListQuery;
import com.techhub.dto.post.PostVO;
import com.techhub.dto.user.UserDetailVO;
import com.techhub.dto.user.UserProfileVO;
import com.techhub.dto.user.UserUpdateRequest;
import com.techhub.security.SecurityUtils;
import com.techhub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<UserDetailVO> getCurrentUser() {
        return R.ok(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public R<Void> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        userService.updateProfile(request);
        return R.ok("更新成功");
    }

    @GetMapping("/{id}")
    public R<UserProfileVO> getUserProfile(@PathVariable Long id) {
        return R.ok(userService.getUserProfile(id));
    }

    @GetMapping("/{id}/posts")
    public R<PageResult<PostVO>> getUserPosts(@PathVariable Long id, PostListQuery query) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return R.ok(userService.getUserPosts(id, query, currentUserId));
    }

    @PatchMapping("/me/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return R.ok("密码修改成功");
    }

    @GetMapping("/me/favorites")
    public R<PageResult<PostVO>> getFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return R.ok(userService.getFavorites(userId, page, size));
    }
}
