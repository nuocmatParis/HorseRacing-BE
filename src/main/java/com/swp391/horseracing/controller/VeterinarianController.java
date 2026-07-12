package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.veterinarian.request.VeterinarianCreationRequest;
import com.swp391.horseracing.dto.veterinarian.request.VeterinarianUpdateRequest;
import com.swp391.horseracing.dto.veterinarian.response.VeterinarianResponse;
import com.swp391.horseracing.service.VeterinarianService;
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
@RequestMapping("/api/veterinarians")
public class VeterinarianController {

    VeterinarianService veterinarianService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VeterinarianResponse> create(@RequestBody @Valid VeterinarianCreationRequest request) {
        return ApiResponse.<VeterinarianResponse>builder()
                .result(veterinarianService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<VeterinarianResponse>> getAll() {
        return ApiResponse.<List<VeterinarianResponse>>builder()
                .result(veterinarianService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<VeterinarianResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<VeterinarianResponse>builder()
                .result(veterinarianService.getById(id))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VeterinarianResponse> update(@PathVariable UUID id,
                                                     @RequestBody @Valid VeterinarianUpdateRequest request) {
        return ApiResponse.<VeterinarianResponse>builder()
                .result(veterinarianService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        veterinarianService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
