package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.HorseOwner.request.OwnerCreationRequest;
import com.swp391.horseracing.dto.HorseOwner.request.OwnerUpdateRequest;
import com.swp391.horseracing.dto.HorseOwner.response.OwnerResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.service.OwnerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    OwnerService ownerService;

    @PostMapping("/profile")
    public ApiResponse<OwnerResponse> createMyProfile(@RequestBody @Valid OwnerCreationRequest request){
        return ApiResponse.<OwnerResponse>builder()
                .result(ownerService.createMyProfile(request))
                .build();
    }

    @PreAuthorize("hasRole('HORSE_OWNER')")
    @PutMapping("/profile")
    public ApiResponse<OwnerResponse> updateMyProfile(@RequestBody @Valid OwnerUpdateRequest request){
        return ApiResponse.<OwnerResponse>builder()
                .result(ownerService.updateMyProfile(request))
                .build();
    }

    @PreAuthorize("hasRole('HORSE_OWNER')")
    @GetMapping("/me")
    public ApiResponse<OwnerResponse> getMyProfile(){
        return ApiResponse.<OwnerResponse>builder()
                .result(ownerService.getMyProfile())
                .build();
    }
}
