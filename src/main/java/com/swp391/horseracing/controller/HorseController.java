package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.horse.request.HorseCreationRequest;
import com.swp391.horseracing.dto.horse.request.HorseUpdateRequest;
import com.swp391.horseracing.dto.horse.response.HorseResponse;
import com.swp391.horseracing.service.HorseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/horses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('HORSE_OWNER')")
public class HorseController {

    HorseService horseService;

    @GetMapping("/my-horses")
    public ApiResponse<List<HorseResponse>> getAll() {
        return ApiResponse.<List<HorseResponse>>builder()
                .result(horseService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<HorseResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<HorseResponse>builder()
                .result(horseService.getById(id))
                .build();
    }

    @PostMapping
    public ApiResponse<HorseResponse> create(@RequestBody @Valid HorseCreationRequest request) {
        return ApiResponse.<HorseResponse>builder()
                .result(horseService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<HorseResponse> update(@PathVariable UUID id,
                                             @RequestBody @Valid HorseUpdateRequest request) {
        return ApiResponse.<HorseResponse>builder()
                .result(horseService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        horseService.delete(id);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/image")
    public ApiResponse<HorseResponse> uploadImage(@PathVariable UUID id,
                                                  @RequestParam("file") @NotNull MultipartFile file) {
        return ApiResponse.<HorseResponse>builder()
                .result(horseService.uploadImage(id, file))
                .build();
    }
}
