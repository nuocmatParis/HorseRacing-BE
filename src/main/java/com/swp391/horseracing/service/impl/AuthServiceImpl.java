package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.auth.request.LoginRequest;
import com.swp391.horseracing.dto.auth.response.AuthResponse;
import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.UserMapper;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    UserMapper userMapper;


    @Override
    public AuthResponse login(LoginRequest request) {
        return null;
    }

    @Override
    public UserResponse getMyProfile() {
        return null;
    }
}
