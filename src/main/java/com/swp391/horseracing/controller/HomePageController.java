package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse;
import com.swp391.horseracing.service.HomePageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HomePageController {

    HomePageService homePageService;

    @GetMapping("/home")
    public ApiResponse<HomePageResponse> getHomePage() {
        return ApiResponse.<HomePageResponse>builder()
                .result(homePageService.getHomePage())
                .build();
    }
}
