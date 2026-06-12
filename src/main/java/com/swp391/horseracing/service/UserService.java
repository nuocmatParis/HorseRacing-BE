package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;

public interface UserService {
    UserResponse create(UserCreationRequest request);
}
