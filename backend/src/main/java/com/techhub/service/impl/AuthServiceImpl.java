package com.techhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.techhub.common.BusinessException;
import com.techhub.common.ResultCode;
import com.techhub.dto.auth.LoginRequest;
import com.techhub.dto.auth.LoginResponse;
import com.techhub.dto.auth.RegisterRequest;
import com.techhub.entity.User;
import com.techhub.mapper.UserMapper;
import com.techhub.security.JwtTokenProvider;
import com.techhub.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // Check unique username
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
        }
        // Check unique email (only when email is provided)
        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email)) > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "邮箱已被注册");
            }
        }
        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail((email != null && !email.isBlank()) ? email : null);
        user.setRole("USER");
        user.setStatus(1);
        // 设置默认头像（DiceBear initials SVG，免费无需 API Key）
        user.setAvatarUrl("https://api.dicebear.com/9.x/initials/svg?seed=" + request.getUsername());
        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Check user exists
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        // Check banned
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "用户已被封禁");
        }
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUserId(user.getId().toString());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        return response;
    }
}
