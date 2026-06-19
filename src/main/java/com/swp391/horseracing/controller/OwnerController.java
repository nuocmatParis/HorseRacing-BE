package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.HorseOwner.request.OwnerCreationRequest;
import com.swp391.horseracing.dto.HorseOwner.request.OwnerUpdateRequest;
import com.swp391.horseracing.dto.HorseOwner.response.OwnerResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.registration.request.RegisterHorseRequest;
import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.service.OwnerService;
import com.swp391.horseracing.service.TournamentRegistrationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/owners")
public class OwnerController {

    OwnerService ownerService;
    TournamentRegistrationService tournamentRegistrationService;

    @PreAuthorize("hasRole('HORSE_OWNER')")
    @PostMapping("/tournaments/{id}/register-horse")
    public ApiResponse<HorseTournamentRegistrationResponse> registerHorse(@PathVariable UUID id,
                                                                           @RequestBody @Valid RegisterHorseRequest request) {
        return ApiResponse.<HorseTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.registerHorse(id, request.getHorseId()))
                .build();
    }

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
