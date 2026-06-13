package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse create(UserCreationRequest request);
    List<UserResponse> findAll();
}
