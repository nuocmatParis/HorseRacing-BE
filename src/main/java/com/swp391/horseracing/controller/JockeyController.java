package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.jockey.request.JockeyCreationRequest;
import com.swp391.horseracing.dto.jockey.request.JockeyUpdateRequest;
import com.swp391.horseracing.dto.jockey.response.JockeyResponse;
import com.swp391.horseracing.service.JockeyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jockeys")
public class JockeyController {

    JockeyService jockeyService;

    @GetMapping
    public ApiResponse<List<JockeyResponse>> getAll(){
        return ApiResponse.<List<JockeyResponse>>builder()
                .result(jockeyService.getAll())
                .build();
    }

    @PostMapping("/profile")
    public ApiResponse<JockeyResponse> createMyProfile(@RequestBody @Valid JockeyCreationRequest request){
        return ApiResponse.<JockeyResponse>builder()
                .result(jockeyService.createMyProfile(request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<JockeyResponse> getById(@PathVariable UUID id){
        return ApiResponse.<JockeyResponse>builder()
                .result(jockeyService.getById(id))
                .build();
    }
    @PreAuthorize("hasRole('JOCKEY')")
    @GetMapping("/me")
    public ApiResponse<JockeyResponse> getMyProfile(){
        return ApiResponse.<JockeyResponse>builder()
                .result(jockeyService.getMyProfile())
                .build();
    }

    @PreAuthorize("hasRole('JOCKEY')")
    @PutMapping("/profile")
    public ApiResponse<JockeyResponse> updateMyProfile(@RequestBody @Valid JockeyUpdateRequest request){
        return ApiResponse.<JockeyResponse>builder()
                .result(jockeyService.updateMyProfile(request))
                .build();
    }


}
