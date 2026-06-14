package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.jockey.request.JockeyCreationRequest;
import com.swp391.horseracing.dto.jockey.response.JockeyResponse;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.service.JockeyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PostMapping
    public ApiResponse<JockeyResponse> create(@RequestBody @Valid JockeyCreationRequest request){
        return ApiResponse.<JockeyResponse>builder()
                .result(jockeyService.create(request))
                .build();
    }
}
