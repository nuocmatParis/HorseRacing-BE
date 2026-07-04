package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.auth.request.ResendOtp;
import com.swp391.horseracing.dto.auth.request.VerifyEmail;
import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> findAll();
    void requestRegisterOtp(UserCreationRequest request);

    UserResponse verifyRegisterOtp(VerifyEmail request);

    void resendOtp(ResendOtp request);
}
