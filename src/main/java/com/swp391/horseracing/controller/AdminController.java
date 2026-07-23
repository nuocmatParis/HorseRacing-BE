package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.medicalstaff.request.AssignInspectionStaffRequest;
import com.swp391.horseracing.dto.medicalstaff.response.InspectionStaffAssignmentResponse;
import com.swp391.horseracing.dto.race_entry.request.AdminCreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;
import com.swp391.horseracing.dto.race_referee.request.CreateRaceRefereeRequest;
import com.swp391.horseracing.dto.race_referee.response.RaceRefereeResponse;
import com.swp391.horseracing.dto.referee.response.RefereeResponse;
import com.swp391.horseracing.dto.registration.response.*;
import com.swp391.horseracing.dto.tournament.request.*;
import com.swp391.horseracing.dto.tournament.response.*;
import com.swp391.horseracing.dto.bracket.BracketPreviewResponse;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.service.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    TournamentService tournamentService;
    RoundService roundService;
    RaceService raceService;
    PrizeStructureService prizeStructureService;
    TournamentEligibilityService tournamentEligibilityService;
    TournamentRegistrationService tournamentRegistrationService;
    ContractService contractService;
    RaceEntryService raceEntryService;
    RaceRefereeService raceRefereeService;
    RefereeService refereeService;
    RaceInspectionStaffService raceInspectionStaffService;
    BracketService bracketService;

    @PostMapping("/tournaments")
    public ApiResponse<TournamentResponse> createTournament(@RequestBody @Valid CreateTournamentRequest request) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.createTournament(request))
                .build();
    }

    @PostMapping("/tournaments/{id}/rounds")
    public ApiResponse<RoundResponse> createRound(@PathVariable UUID id,
                                                  @RequestBody @Valid CreateRoundRequest request) {
        return ApiResponse.<RoundResponse>builder()
                .result(roundService.create(id, request))
                .build();
    }

    @PostMapping("/rounds/{roundId}/races")
    public ApiResponse<RaceResponse> createRace(@PathVariable UUID roundId,
                                                @RequestBody @Valid CreateRaceRequest request) {
        return ApiResponse.<RaceResponse>builder()
                .result(raceService.create(roundId, request))
                .build();
    }

    @PostMapping("/tournaments/{id}/prize-structures")
    public ApiResponse<PrizeStructureResponse> createPrizeStructure(@PathVariable UUID id,
                                                                    @RequestBody @Valid CreatePrizeStructureRequest request) {
        return ApiResponse.<PrizeStructureResponse>builder()
                .result(prizeStructureService.create(id, request))
                .build();
    }

    @PostMapping("/tournaments/{id}/eligibility")
    public ApiResponse<TournamentEligibilityResponse> createEligibility(@PathVariable UUID id,
                                                                        @RequestBody @Valid CreateEligibilityRequest request) {
        return ApiResponse.<TournamentEligibilityResponse>builder()
                .result(tournamentEligibilityService.create(id, request))
                .build();
    }
    @PostMapping("/tournaments/{id}/publish")
    public ApiResponse<TournamentResponse> publishTournament(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.publish(id))
                .build();
    }

    @GetMapping("/horse-registrations")
    public ApiResponse<List<HorseTournamentRegistrationResponse>> getAllHorseRegistrations(){
        return ApiResponse.<List<HorseTournamentRegistrationResponse>>builder()
                .result(tournamentRegistrationService.getAllHorseRegistrations())
                .build();
    }

    @GetMapping("/jockey-registrations")
    public ApiResponse<List<JockeyTournamentRegistrationResponse>> getAllJockeyRegistrations(){
        return ApiResponse.<List<JockeyTournamentRegistrationResponse>>builder()
                .result(tournamentRegistrationService.getAllJockeyRegistrations())
                .build();
    }

    @PostMapping("/tournaments/{id}/complete-review")
    public ApiResponse<TournamentResponse> completeReview(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.completeReview(id))
                .build();
    }

    @PostMapping("/tournaments/{id}/complete-matching")
    public ApiResponse<TournamentResponse> completeMatching(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.completeMatching(id))
                .build();
    }

    @PostMapping("/tournaments/{id}/publish-schedule")
    public ApiResponse<TournamentResponse> publishSchedule(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.publishSchedule(id))
                .build();
    }

    @PostMapping("/tournaments/{id}/publish-results")
    public ApiResponse<TournamentResponse> publishResults(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.publishResults(id))
                .build();
    }

    @PostMapping("/horse-registrations/{id}/approve")
    public ApiResponse<HorseTournamentRegistrationResponse> approveHorseRegistration(@PathVariable UUID id) {
        return ApiResponse.<HorseTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.approveHorseRegistration(id))
                .build();
    }

    @PostMapping("/horse-registrations/{id}/reject")
    public ApiResponse<HorseTournamentRegistrationResponse> rejectHorseRegistration(@PathVariable UUID id,
                                                                                    @RequestBody(required = false) String reason) {
        return ApiResponse.<HorseTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.rejectHorseRegistration(id, reason))
                .build();
    }




    @PostMapping("/races/{raceId}/entries")
    public ApiResponse<RaceEntryResponse> createRaceEntry(@PathVariable UUID raceId,
                                                           @RequestBody @Valid AdminCreateRaceEntryRequest request) {
        CreateRaceEntryRequest serviceRequest = CreateRaceEntryRequest.builder()
                .raceId(raceId)
                .contractId(request.getContractId())
                .laneNumber(request.getLaneNumber())
                .build();
        return ApiResponse.<RaceEntryResponse>builder()
                .result(raceEntryService.create(serviceRequest))
                .build();
    }

    @DeleteMapping("/race-entries/{entryId}")
    public ApiResponse<Void> deleteRaceEntry(@PathVariable UUID entryId) {
        raceEntryService.delete(entryId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/referees")
    public ApiResponse<List<RefereeResponse>> getAllReferees(
            @RequestParam(required = false) RefereeStatus status) {
        return ApiResponse.<List<RefereeResponse>>builder()
                .result(refereeService.getAllReferees(status))
                .build();
    }

    @PostMapping("/races/{raceId}/referees")
    public ApiResponse<RaceRefereeResponse> assignReferee(@PathVariable UUID raceId,
                                                          @RequestBody CreateRaceRefereeRequest request) {
        request.setRaceId(raceId);
        return ApiResponse.<RaceRefereeResponse>builder()
                .result(raceRefereeService.create(request))
                .build();
    }

    @DeleteMapping("/races/{raceId}/referees/{refereeId}")
    public ApiResponse<Void> removeReferee(@PathVariable UUID raceId, @PathVariable UUID refereeId) {
        raceRefereeService.deleteByRaceAndReferee(raceId, refereeId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/races/{raceId}/publish-schedule")
    public ApiResponse<RaceResponse> publishRaceSchedule(@PathVariable UUID raceId) {
        return ApiResponse.<RaceResponse>builder()
                .result(raceService.publishSchedule(raceId))
                .build();
    }


    @PutMapping("/tournaments/{id}")
    public ApiResponse<TournamentResponse> updateTournament(@PathVariable UUID id,
                                                            @RequestBody @Valid UpdateTournamentRequest request) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.update(id, request))
                .build();
    }

    @PutMapping("/rounds/{roundId}")
    public ApiResponse<RoundResponse> updateRound(@PathVariable UUID roundId,
                                                  @RequestBody @Valid UpdateRoundRequest request) {
        return ApiResponse.<RoundResponse>builder()
                .result(roundService.update(roundId, request))
                .build();
    }

    @PutMapping("/races/{raceId}")
    public ApiResponse<RaceResponse> updateRace(@PathVariable UUID raceId,
                                                @RequestBody @Valid UpdateRaceRequest request) {
        return ApiResponse.<RaceResponse>builder()
                .result(raceService.update(raceId, request))
                .build();
    }

    @PutMapping("/prize-structures/{prizeStructureId}")
    public ApiResponse<PrizeStructureResponse> updatePrizeStructure(@PathVariable UUID prizeStructureId,
                                                                    @RequestBody @Valid UpdatePrizeStructureRequest request) {
        return ApiResponse.<PrizeStructureResponse>builder()
                .result(prizeStructureService.update(prizeStructureId, request))
                .build();
    }

    @PutMapping("/eligibility/{eligibilityId}")
    public ApiResponse<TournamentEligibilityResponse> updateEligibility(@PathVariable UUID eligibilityId,
                                                                        @RequestBody @Valid UpdateEligibilityRequest request) {
        return ApiResponse.<TournamentEligibilityResponse>builder()
                .result(tournamentEligibilityService.update(eligibilityId, request))
                .build();
    }

    @DeleteMapping("/tournaments/{id}")
    public ApiResponse<Void> deleteTournament(@PathVariable UUID id) {
        tournamentService.delete(id);
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/rounds/{roundId}")
    public ApiResponse<Void> deleteRound(@PathVariable UUID roundId) {
        roundService.delete(roundId);
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/races/{raceId}")
    public ApiResponse<Void> deleteRace(@PathVariable UUID raceId) {
        raceService.delete(raceId);
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/prize-structures/{prizeStructureId}")
    public ApiResponse<Void> deletePrizeStructure(@PathVariable UUID prizeStructureId) {
        prizeStructureService.delete(prizeStructureId);
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/eligibility/{eligibilityId}")
    public ApiResponse<Void> deleteEligibility(@PathVariable UUID eligibilityId) {
        tournamentEligibilityService.delete(eligibilityId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/contracts")
    public ApiResponse<PageResponse<ContractResponse>> getContractsByStatus(
            @RequestParam ContractStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ContractResponse>>builder()
                .result(contractService.getContractsByStatus(status, page, size))
                .build();
    }

    @GetMapping("/contracts/approved/tournaments/{tournamentId}")
    public ApiResponse<PageResponse<ContractResponse>> getApprovedContractsByTournament(
            @PathVariable UUID tournamentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ContractResponse>>builder()
                .result(contractService.getApprovedContractsByTournament(tournamentId, page, size))
                .build();
    }

    @PostMapping("/tournaments/{id}/close-registration")
    public ApiResponse<TournamentResponse> closeRegistration(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.closeRegistration(id))
                .build();
    }

    @PostMapping("/contracts/{id}/release-final-payout")
    public ApiResponse<ContractResponse> releaseFinalPayout(@PathVariable UUID id){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.releaseFinalPayout(id))
                .build();
    }

    @PostMapping("/races/{raceId}/inspection-staff/assign")
    public ApiResponse<InspectionStaffAssignmentResponse> assign(@PathVariable UUID raceId,
                                                                 @Valid @RequestBody AssignInspectionStaffRequest request){
        return ApiResponse.<InspectionStaffAssignmentResponse>builder()
                .result(raceInspectionStaffService.assign(raceId, request))
                .build();
    }

    @PostMapping("/races/{raceId}/inspection-staff/auto-assign")
    public ApiResponse<InspectionStaffAssignmentResponse> assign(@PathVariable UUID raceId){
        return ApiResponse.<InspectionStaffAssignmentResponse>builder()
                .result(raceInspectionStaffService.autoAssign(raceId))
                .build();
    }

    @PostMapping("/races/{raceId}/postpone")
    public ApiResponse<RaceResponse> postponeRace(@PathVariable UUID raceId,
                                                  @Valid @RequestBody com.swp391.horseracing.dto.tournament.request.RescheduleRaceRequest request) {
        return ApiResponse.<RaceResponse>builder()
                .result(raceService.rescheduleRace(raceId, request))
                .build();
    }

    @PostMapping("/races/{raceId}/cancel")
    public ApiResponse<Void> cancelRace(@PathVariable UUID raceId,
                                        @Valid @RequestBody com.swp391.horseracing.dto.tournament.request.CancelRaceRequest request) {
        raceService.cancelRace(raceId, request);
        return ApiResponse.<Void>builder()
                .message("Race cancelled successfully")
                .build();
    }

    @GetMapping("/races/{raceId}/reschedule-proposals")
    public ApiResponse<List<java.time.LocalDateTime>> getRescheduleProposals(@PathVariable UUID raceId) {
        return ApiResponse.<List<java.time.LocalDateTime>>builder()
                .result(raceService.getRescheduleProposals(raceId))
                .build();
    }

    @GetMapping("/phase-timing-defaults")
    public ApiResponse<Map<String, Integer>> getPhaseTimingDefaults(@RequestParam int capacity) {
        return ApiResponse.<Map<String, Integer>>builder()
                .result(tournamentService.getDefaultPhaseConfigs(capacity))
                .build();
    }

    @GetMapping("/tournaments/{id}/bracket-preview")
    public ApiResponse<BracketPreviewResponse> previewBracket(
            @PathVariable UUID id,
            @RequestParam int actualEntries) {
        return ApiResponse.<BracketPreviewResponse>builder()
                .result(bracketService.preview(id, actualEntries))
                .build();
    }

    @PostMapping("/tournaments/{id}/bracket-confirm")
    public ApiResponse<Void> confirmBracket(@PathVariable UUID id) {
        bracketService.confirm(id);
        return ApiResponse.<Void>builder()
                .message("Bracket confirmed and rounds/races created")
                .build();
    }

    @PutMapping("/tournaments/{id}/bracket-recalculate")
    public ApiResponse<Void> recalculateBracket(
            @PathVariable UUID id,
            @RequestParam int actualEntries) {
        bracketService.recalculate(id, actualEntries);
        return ApiResponse.<Void>builder()
                .message("Bracket recalculated with " + actualEntries + " entries")
                .build();
    }

}
