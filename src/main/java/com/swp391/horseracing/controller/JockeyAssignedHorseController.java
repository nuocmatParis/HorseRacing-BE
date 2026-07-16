package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.jockey.response.JockeyAssignedHorseResponse;
import com.swp391.horseracing.service.JockeyAssignedHorseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jockey/horses")
@PreAuthorize("hasRole('JOCKEY')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JockeyAssignedHorseController {
    JockeyAssignedHorseService jockeyAssignedHorseService;

    @GetMapping("/assigned")
    public ApiResponse<List<JockeyAssignedHorseResponse>> getMyAssignedHorses() {
        return ApiResponse.<List<JockeyAssignedHorseResponse>>builder()
                .result(jockeyAssignedHorseService.getMyAssignedHorses())
                .build();
    }
}
