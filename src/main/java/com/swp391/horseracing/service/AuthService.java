package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.auth.request.AuthRequest;
import com.swp391.horseracing.dto.auth.response.AuthResponse;
import com.swp391.horseracing.dto.user.response.UserResponse;
import com.swp391.horseracing.entity.User;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    UserResponse getMyProfile();

    String generateToken(User user);

}
