package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.referee.request.RefereeCreationRequest;
import com.swp391.horseracing.dto.referee.request.RefereeUpdateRequest;
import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.service.RefereeService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/referees")
public class RefereeController {

    RefereeService refereeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RefereeResponse> create(@RequestBody @Valid RefereeCreationRequest request) {
        return ApiResponse.<RefereeResponse>builder()
                .result(refereeService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<RefereeResponse>> getAll(@RequestParam(required = false) RefereeStatus status) {
        if (status != null) {
            return ApiResponse.<List<RefereeResponse>>builder()
                    .result(refereeService.getAllReferees(status))
                    .build();
        }
        return ApiResponse.<List<RefereeResponse>>builder()
                .result(refereeService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RefereeResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<RefereeResponse>builder()
                .result(refereeService.getById(id))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RefereeResponse> update(@PathVariable UUID id,
                                                @RequestBody @Valid RefereeUpdateRequest request) {
        return ApiResponse.<RefereeResponse>builder()
                .result(refereeService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        refereeService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
