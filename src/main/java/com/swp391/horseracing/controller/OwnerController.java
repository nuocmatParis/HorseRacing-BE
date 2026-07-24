package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.horseowner.request.OwnerCreationRequest;
import com.swp391.horseracing.dto.horseowner.request.OwnerUpdateRequest;
import com.swp391.horseracing.dto.horseowner.response.OwnerResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.registration.request.RegisterHorseRequest;
import com.swp391.horseracing.dto.registration.request.WithdrawRegistrationRequest;
import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.dto.registration.response.JockeyTournamentRegistrationResponse;
import com.swp391.horseracing.service.OwnerService;
import com.swp391.horseracing.service.TournamentRegistrationService;
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

    @PreAuthorize("hasRole('HORSE_OWNER')")
    @GetMapping("/tournaments/{id}/accepted-jockeys")
    public ApiResponse<List<JockeyTournamentRegistrationResponse>> getAcceptedJockeys(@PathVariable UUID id) {
        return ApiResponse.<List<JockeyTournamentRegistrationResponse>>builder()
                .result(tournamentRegistrationService.getApprovedJockeysByTournament(id))
                .build();
    }

    @PreAuthorize("hasRole('HORSE_OWNER')")
    @GetMapping("/my-registrations")
    public ApiResponse<List<HorseTournamentRegistrationResponse>> getMyHorseRegistration(){
        return ApiResponse.<List<HorseTournamentRegistrationResponse>>builder()
                .result(tournamentRegistrationService.getMyHorseRegistrations())
                .build();
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('HORSE_OWNER')")
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

    @PreAuthorize("hasRole('HORSE_OWNER')")
    @PostMapping("/registrations/{id}/withdraw")
    public ApiResponse<HorseTournamentRegistrationResponse> withdrawRegistration(
            @PathVariable UUID id,
            @RequestBody @Valid WithdrawRegistrationRequest request) {
        return ApiResponse.<HorseTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.withdrawHorseRegistration(id, request.getReason()))
                .build();
    }
}
