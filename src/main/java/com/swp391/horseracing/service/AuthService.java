package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.auth.request.LoginRequest;
import com.swp391.horseracing.dto.auth.response.AuthResponse;
import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;

public interface AuthService {

    /**
     * Đăng nhập.
     * - Verify password
     * - Update lastLoginAt
     * - Trả về JWT token
     */
    AuthResponse login(LoginRequest request);

    /**
     * Lấy thông tin user hiện tại từ Security context.
     */
    UserResponse getMyProfile();
}
