package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffCreationRequest;
import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffUpdateRequest;
import com.swp391.horseracing.dto.medical_staff.response.MedicalStaffResponse;
import com.swp391.horseracing.service.MedicalStaffService;
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
@RequestMapping("/api/medical-staff")
public class MedicalStaffController {

    MedicalStaffService medicalStaffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicalStaffResponse> create(@RequestBody @Valid MedicalStaffCreationRequest request) {
        return ApiResponse.<MedicalStaffResponse>builder()
                .result(medicalStaffService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<MedicalStaffResponse>> getAll() {
        return ApiResponse.<List<MedicalStaffResponse>>builder()
                .result(medicalStaffService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicalStaffResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<MedicalStaffResponse>builder()
                .result(medicalStaffService.getById(id))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MedicalStaffResponse> update(@PathVariable UUID id,
                                                     @RequestBody @Valid MedicalStaffUpdateRequest request) {
        return ApiResponse.<MedicalStaffResponse>builder()
                .result(medicalStaffService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        medicalStaffService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
