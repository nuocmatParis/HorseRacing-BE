package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.race_portal.AssignedRaceResponse;
import com.swp391.horseracing.dto.race_portal.RaceEntryViewResponse;
import com.swp391.horseracing.dto.race_portal.RaceResultItemResponse;
import com.swp391.horseracing.dto.race_portal.RaceResultsResponse;
import com.swp391.horseracing.dto.race_portal.RaceScheduleResponse;
import com.swp391.horseracing.dto.race_portal.RaceSummaryResponse;
import com.swp391.horseracing.dto.race_portal.SpectatorRaceDetailResponse;
import com.swp391.horseracing.dto.race_portal.TournamentInspectionConditionsResponse;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;
import com.swp391.horseracing.entity.MedicalStaff;
import com.swp391.horseracing.entity.AIPrediction;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceInspectionAssignment;
import com.swp391.horseracing.entity.RaceReferee;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.TournamentEligibility;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Veterinarian;
import com.swp391.horseracing.entity.HorseInspection;
import com.swp391.horseracing.entity.JockeyInspection;
import com.swp391.horseracing.enums.AIPredictionPublicationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.MedicalStaffRepository;
import com.swp391.horseracing.repository.AIPredictionRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceInspectionStaffAssignmentRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.VeterinarianRepository;
import com.swp391.horseracing.repository.HorseInspectionRepository;
import com.swp391.horseracing.repository.JockeyInspectionRepository;
import com.swp391.horseracing.service.RacePortalService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RacePortalServiceImpl implements RacePortalService {
    RaceRepository raceRepository;
    RaceEntryRepository raceEntryRepository;
    RaceResultRepository raceResultRepository;
    RaceReportRepository raceReportRepository;
    RaceRefereeRepository raceRefereeRepository;
    RaceInspectionStaffAssignmentRepository inspectionAssignmentRepository;
    RefereeRepository refereeRepository;
    VeterinarianRepository veterinarianRepository;
    MedicalStaffRepository medicalStaffRepository;
    TournamentRepository tournamentRepository;
    UserCurrentService userCurrentService;
    HorseInspectionRepository horseInspectionRepository;
    JockeyInspectionRepository jockeyInspectionRepository;
    AIPredictionRepository aiPredictionRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceScheduleResponse> getOwnerSchedule(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findUpcomingForOwner(
                user.getUserId(), LocalDateTime.now(), PageRequest.of(page, size));
        List<RaceScheduleResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Owner_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toScheduleResponse(race, entries));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceScheduleResponse> getJockeySchedule(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findUpcomingForJockey(
                user.getUserId(), LocalDateTime.now(), PageRequest.of(page, size));
        List<RaceScheduleResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Jockey_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toScheduleResponse(race, entries));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceScheduleResponse> getOwnerRaces(
            LocalDateTime from, LocalDateTime to, int page, int size) {
        validatePage(page, size);
        validateDateRange(from, to);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findParticipantRacesForOwner(
                user.getUserId(), from, to, PageRequest.of(page, size));
        List<RaceScheduleResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Owner_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toScheduleResponse(race, entries));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceScheduleResponse> getJockeyRaces(
            LocalDateTime from, LocalDateTime to, int page, int size) {
        validatePage(page, size);
        validateDateRange(from, to);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findParticipantRacesForJockey(
                user.getUserId(), from, to, PageRequest.of(page, size));
        List<RaceScheduleResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Jockey_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toScheduleResponse(race, entries));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceResultsResponse> getOwnerResults(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findPublishedResultsForOwner(
                user.getUserId(), PageRequest.of(page, size));
        List<RaceResultsResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Owner_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toResultsResponse(race, entries, true));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceResultsResponse> getJockeyResults(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findPublishedResultsForJockey(
                user.getUserId(), PageRequest.of(page, size));
        List<RaceResultsResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Jockey_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toResultsResponse(race, entries, false));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceResultsResponse> getOwnerProvisionalResults(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findProvisionalResultsForOwner(
                user.getUserId(), PageRequest.of(page, size));
        List<RaceResultsResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Owner_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toResultsResponse(race, entries, true, true));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceResultsResponse> getJockeyProvisionalResults(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Page<Race> races = raceRepository.findProvisionalResultsForJockey(
                user.getUserId(), PageRequest.of(page, size));
        List<RaceResultsResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            List<RaceEntry> entries = raceEntryRepository
                    .findByRace_RaceIdAndContract_Jockey_User_UserIdOrderByLaneNumberAsc(
                            race.getRaceId(), user.getUserId());
            items.add(toResultsResponse(race, entries, false, true));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RaceSummaryResponse> getUpcomingRaces(
            LocalDateTime from, LocalDateTime to, UUID tournamentId, int page, int size) {
        validatePage(page, size);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromTime = from == null || from.isBefore(now) ? now : from;
        if (to != null && to.isBefore(fromTime)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (tournamentId != null && !tournamentRepository.existsById(tournamentId)) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_FOUND);
        }

        Page<Race> races = raceRepository.findUpcomingForSpectator(
                fromTime, to, tournamentId, PageRequest.of(page, size));
        List<RaceSummaryResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            items.add(toRaceSummary(race));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public SpectatorRaceDetailResponse getSpectatorRaceDetail(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        if (race.getSchedulePublishedAt() == null) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_PUBLISHED);
        }
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        Map<UUID, AIPrediction> predictionByEntryId = new HashMap<>();
        if (race.getStatus() != RoundStatus.CANCELLED
                && race.getAiPredictionPublicationStatus() == AIPredictionPublicationStatus.PUBLISHED) {
            List<AIPrediction> predictions = aiPredictionRepository.findByEntry_Race_RaceIdOrderByCreatedAtAsc(raceId);
            for (AIPrediction prediction : predictions) {
                if (prediction.getEntry() != null) {
                    predictionByEntryId.put(prediction.getEntry().getEntryId(), prediction);
                } else {
                    log.warn("AI prediction {} has null entry reference, skipping", prediction.getPredictionId());
                }
            }
        }
        return SpectatorRaceDetailResponse.builder()
                .race(toRaceSummary(race))
                .cancelledAt(race.getCancelledAt())
                .cancellationReason(race.getCancellationReason())
                .rescheduledAt(race.getRescheduledAt())
                .rescheduleReason(race.getRescheduleReason())
                .entries(toEntryResponses(entries, predictionByEntryId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignedRaceResponse> getRefereeAssignedRaces(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));
        Page<Race> races = raceRepository.findCurrentForReferee(
                referee.getRefereeId(), LocalDateTime.now(), PageRequest.of(page, size));
        List<AssignedRaceResponse> items = new ArrayList<>();
        for (Race race : races.getContent()) {
            String role = "REFEREE";
            LocalDateTime assignedAt = null;
            if (race.getRound().getHeadReferee() != null
                    && race.getRound().getHeadReferee().getRefereeId().equals(referee.getRefereeId())) {
                role = "HEAD_REFEREE";
                assignedAt = race.getRound().getHeadRefereeAssignedAt();
            } else {
                Optional<RaceReferee> assignment = raceRefereeRepository
                        .findByRace_RaceIdAndReferee_RefereeId(race.getRaceId(), referee.getRefereeId());
                if (assignment.isPresent()) {
                    assignedAt = assignment.get().getAssignedAt();
                }
            }
            items.add(toAssignedResponse(race, role, assignedAt));
        }
        return toPageResponse(races, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignedRaceResponse> getVeterinarianAssignedRaces(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        Veterinarian veterinarian = veterinarianRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.VETERINARIAN_PROFILE_NOT_FOUND));
        Page<RaceInspectionAssignment> assignments = inspectionAssignmentRepository
                .findCurrentForVeterinarian(veterinarian.getVetId(), LocalDateTime.now(), PageRequest.of(page, size));
        List<AssignedRaceResponse> items = new ArrayList<>();
        for (RaceInspectionAssignment assignment : assignments.getContent()) {
            items.add(toAssignedResponse(assignment.getRace(), "VETERINARIAN", assignment.getAssignedAt()));
        }
        return toAssignmentPageResponse(assignments, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssignedRaceResponse> getMedicalAssignedRaces(int page, int size) {
        validatePage(page, size);
        User user = userCurrentService.getCurrentUser();
        MedicalStaff medicalStaff = medicalStaffRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_PROFILE_NOT_FOUND));
        Page<RaceInspectionAssignment> assignments = inspectionAssignmentRepository
                .findCurrentForMedicalStaff(medicalStaff.getMedStaffId(), LocalDateTime.now(), PageRequest.of(page, size));
        List<AssignedRaceResponse> items = new ArrayList<>();
        for (RaceInspectionAssignment assignment : assignments.getContent()) {
            items.add(toAssignedResponse(assignment.getRace(), "MEDICAL_STAFF", assignment.getAssignedAt()));
        }
        return toAssignmentPageResponse(assignments, items);
    }

    private RaceScheduleResponse toScheduleResponse(Race race, List<RaceEntry> entries) {
        return RaceScheduleResponse.builder()
                .race(toRaceSummary(race))
                .myEntries(toEntryResponses(entries))
                .build();
    }

    private RaceResultsResponse toResultsResponse(Race race, List<RaceEntry> entries, boolean ownerView) {
        return toResultsResponse(race, entries, ownerView, false);
    }

    private RaceResultsResponse toResultsResponse(
            Race race, List<RaceEntry> entries, boolean ownerView, boolean provisional) {
        Optional<RaceReport> optionalReport = raceReportRepository.findByRace_RaceId(race.getRaceId());
        if (!provisional && optionalReport.isEmpty()) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_FOUND);
        }
        RaceReport report = optionalReport.orElse(null);
        List<RaceResultItemResponse> results = new ArrayList<>();
        for (RaceEntry entry : entries) {
            Optional<RaceResult> optionalResult = raceResultRepository.findByEntry_EntryId(entry.getEntryId());
            if (optionalResult.isEmpty()) {
                continue;
            }
            RaceResult result = optionalResult.get();
            BigDecimal myPrize = ownerView ? result.getOwnerPrizeAmount() : result.getJockeyPrizeAmount();
            results.add(RaceResultItemResponse.builder()
                    .resultId(result.getResultId())
                    .entry(toEntryResponse(entry))
                    .finishTime(result.getFinishTime())
                    .rank(result.getRank())
                    .status(result.getStatus())
                    .prizeMoney(result.getPrizeMoney())
                    .myPrizeAmount(myPrize)
                    .prizeStatus(result.getPrizeStatus())
                    .prizePaid(result.isPrizePaid())
                    .build());
        }
        return RaceResultsResponse.builder()
                .race(toRaceSummary(race))
                .reportId(report == null ? null : report.getReportId())
                .publishedAt(report == null ? null : report.getPublishedAt())
                .reportStatus(report == null ? null : report.getStatus())
                .provisional(provisional)
                .myResults(results)
                .build();
    }

    private AssignedRaceResponse toAssignedResponse(Race race, String role, LocalDateTime assignedAt) {
        Tournament tournament = race.getRound().getTournament();
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(race.getRaceId());
        return AssignedRaceResponse.builder()
                .race(toRaceSummary(race))
                .assignmentRole(role)
                .assignedAt(assignedAt)
                .inspectionOpenAt(race.getStartTime().minusMinutes(tournament.getInspectionOpenMinutesBefore()))
                .inspectionCloseAt(race.getStartTime().minusMinutes(tournament.getInspectionCloseMinutesBefore()))
                .tournamentConditions(toInspectionConditions(tournament))
                .entryCount(entries.size())
                .entries(toEntryResponses(entries))
                .build();
    }

    private TournamentInspectionConditionsResponse toInspectionConditions(Tournament tournament) {
        List<TournamentEligibilityResponse> rules = new ArrayList<>();
        if (tournament.getEligibilityRules() != null) {
            for (TournamentEligibility rule : tournament.getEligibilityRules()) {
                if (!rule.isActive()) {
                    continue;
                }
                rules.add(TournamentEligibilityResponse.builder()
                        .eligibilityId(rule.getEligibilityId())
                        .targetType(rule.getTargetType())
                        .conditionName(rule.getConditionName())
                        .conditionOperator(rule.getConditionOperator())
                        .conditionValue(rule.getConditionValue())
                        .isActive(rule.isActive())
                        .tournamentId(tournament.getTournamentId())
                        .build());
            }
        }
        return TournamentInspectionConditionsResponse.builder()
                .allowedBreed(tournament.getAllowedBreed())
                .minHorseAge(tournament.getMinHorseAge())
                .maxHorseAge(tournament.getMaxHorseAge())
                .raceClass(tournament.getRaceClass())
                .distance(tournament.getDistance())
                .handicapEnabled(tournament.isHandicapEnabled())
                .topWeightLbs(tournament.getTopWeightLbs())
                .minWeightLbs(tournament.getMinWeightLbs())
                .equipmentWeightKg(tournament.getEquipmentWeightKg())
                .eligibilityRules(rules)
                .build();
    }

    private RaceSummaryResponse toRaceSummary(Race race) {
        return RaceSummaryResponse.builder()
                .tournamentId(race.getRound().getTournament().getTournamentId())
                .tournamentName(race.getRound().getTournament().getName())
                .roundId(race.getRound().getRoundId())
                .roundName(race.getRound().getRoundName())
                .raceId(race.getRaceId())
                .raceName(race.getName())
                .startTime(race.getStartTime())
                .endTime(race.getEndTime())
                .trackCondition(race.getTrackCondition())
                .distance(race.getDistance())
                .sequenceOrder(race.getSequenceOrder())
                .status(race.getStatus())
                .predictionOpenAt(race.getPredictionOpenAt())
                .predictionCloseAt(race.getPredictionCloseAt())
                .schedulePublishedAt(race.getSchedulePublishedAt())
                .build();
    }

    private List<RaceEntryViewResponse> toEntryResponses(List<RaceEntry> entries) {
        List<RaceEntryViewResponse> responses = new ArrayList<>();
        for (RaceEntry entry : entries) {
            responses.add(toEntryResponse(entry));
        }
        return responses;
    }

    private List<RaceEntryViewResponse> toEntryResponses(
            List<RaceEntry> entries,
            Map<UUID, AIPrediction> predictionByEntryId) {
        List<RaceEntryViewResponse> responses = new ArrayList<>();
        for (RaceEntry entry : entries) {
            RaceEntryViewResponse response = toEntryResponse(entry);
            AIPrediction prediction = predictionByEntryId.get(entry.getEntryId());
            if (prediction != null) {
                response.setWinProbability(prediction.getWinProbability());
                response.setTopNProbability(prediction.getTopNProbability());
                response.setConfidenceScore(prediction.getConfidenceScore());
                response.setPredictionReason(prediction.getPredictionReason());
            }
            responses.add(response);
        }
        return responses;
    }

    private RaceEntryViewResponse toEntryResponse(RaceEntry entry) {
        RaceEntryViewResponse.RaceEntryViewResponseBuilder builder = RaceEntryViewResponse.builder()
                .entryId(entry.getEntryId())
                .laneNumber(entry.getLaneNumber())
                .status(entry.getStatus())
                .horseId(entry.getContract().getHorse().getHorseId())
                .horseName(entry.getContract().getHorse().getName())
                .horseBreed(entry.getContract().getHorse().getBreed())
                .horseRegisteredWeight(entry.getContract().getHorse().getWeight())
                .jockeyId(entry.getContract().getJockey().getJockeyId())
                .jockeyName(entry.getContract().getJockey().getUser().getFullName())
                .jockeyRegisteredWeight(entry.getContract().getJockey().getWeight())
                .scratchedReason(entry.getScratchedReason())
                .disqualifiedReason(entry.getDisqualifiedReason());

        Optional<HorseInspection> horseInspection = horseInspectionRepository
                .findByRaceEntry_EntryId(entry.getEntryId());
        if (horseInspection.isPresent()) {
            builder.horseInspectionId(horseInspection.get().getHorseInspectionId())
                    .horseInspectionStatus(horseInspection.get().getStatus())
                    .horseInspectionResult(horseInspection.get().getResult());
        }

        Optional<JockeyInspection> jockeyInspection = jockeyInspectionRepository
                .findByRaceEntry_EntryId(entry.getEntryId());
        if (jockeyInspection.isPresent()) {
            builder.jockeyInspectionId(jockeyInspection.get().getJockeyInspectionId())
                    .jockeyInspectionStatus(jockeyInspection.get().getStatus())
                    .jockeyInspectionResult(jockeyInspection.get().getResult());
        }
        return builder.build();
    }

    private <T> PageResponse<T> toPageResponse(Page<Race> source, List<T> items) {
        return new PageResponse<>(items, source.getNumber(), source.getSize(), source.getTotalElements(),
                source.getTotalPages(), source.isFirst(), source.isLast());
    }

    private PageResponse<AssignedRaceResponse> toAssignmentPageResponse(
            Page<RaceInspectionAssignment> source, List<AssignedRaceResponse> items) {
        return new PageResponse<>(items, source.getNumber(), source.getSize(), source.getTotalElements(),
                source.getTotalPages(), source.isFirst(), source.isLast());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
