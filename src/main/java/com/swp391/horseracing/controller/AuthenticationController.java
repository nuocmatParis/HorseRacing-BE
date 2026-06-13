package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.auth.request.AuthRequest;
import com.swp391.horseracing.dto.auth.response.AuthResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.user.request.UserCreationRequest;
import com.swp391.horseracing.dto.user.response.UserResponse;
import com.swp391.horseracing.service.AuthService;
import com.swp391.horseracing.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthenticationController {
    UserService userService;
    AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> createUser(
            @RequestBody @Valid UserCreationRequest request){
        return ApiResponse.<UserResponse>builder()
                .result(userService.create(request))
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @RequestBody @Valid AuthRequest request){
        return ApiResponse.<AuthResponse>builder()
                .result(authService.login(request))
                .build();
    }

    @GetMapping("/me")
    public  ApiResponse<UserResponse> getMyProfile(){
        return ApiResponse.<UserResponse>builder()
                .result(authService.getMyProfile())
                .build();
    }


}
