package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateRaceRequest;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.dto.race.response.RaceEntryReadinessResponse;
import com.swp391.horseracing.dto.race.response.RaceStartReadinessResponse;
import com.swp391.horseracing.dto.race.response.RaceStartWindowResponse;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.HorseInspection;
import com.swp391.horseracing.entity.JockeyInspection;
import com.swp391.horseracing.entity.RaceInspectionAssignment;
import com.swp391.horseracing.entity.Veterinarian;
import com.swp391.horseracing.entity.MedicalStaff;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceMapper;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.HorseInspectionRepository;
import com.swp391.horseracing.repository.JockeyInspectionRepository;
import com.swp391.horseracing.repository.RaceInspectionStaffAssignmentRepository;
import com.swp391.horseracing.repository.VeterinarianRepository;
import com.swp391.horseracing.repository.MedicalStaffRepository;
import com.swp391.horseracing.service.RaceService;
import com.swp391.horseracing.service.PredictionService;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swp391.horseracing.entity.RaceReferee;
import java.util.ArrayList;
import java.util.Optional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.swp391.horseracing.entity.Tournament;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceServiceImpl implements RaceService {

    RaceRepository raceRepository;
    RoundRepository roundRepository;
    UserRepository userRepository;
    RaceEntryRepository raceEntryRepository;
    RaceRefereeRepository raceRefereeRepository;
    RaceMapper raceMapper;
    RefereeRepository refereeRepository;
    HorseInspectionRepository horseInspectionRepository;
    JockeyInspectionRepository jockeyInspectionRepository;
    RaceInspectionStaffAssignmentRepository raceInspectionStaffAssignmentRepository;
    VeterinarianRepository veterinarianRepository;
    MedicalStaffRepository medicalStaffRepository;
    PredictionService predictionService;
    BusinessNotificationEventService notificationEventService;

    @Override
    @Transactional
    public RaceResponse create(UUID roundId, CreateRaceRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        if (round.getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
        if (round.getTournament().getBracketPlanStatus() != BracketPlanStatus.NOT_GENERATED) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
        }

        Tournament tournament = round.getTournament();
        LocalDateTime endTime = request.getStartTime().plusMinutes(tournament.getDefaultRaceOperationalMinutes());

        if (!request.getStartTime().toLocalDate().equals(endTime.toLocalDate())) {
            throw new AppException(ErrorCode.INVALID_RACE_DATES);
        }

        if (request.getStartTime().isBefore(round.getStartDate())
                || endTime.isAfter(round.getEndDate())) {
            throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
        }

        if (raceRepository.existsByRound_RoundIdAndName(roundId, request.getName())) {
            throw new AppException(ErrorCode.RACE_NAME_ALREADY_EXISTS);
        }
        if(round.getMaxRaces() <= round.getRaces().size()){
            throw new AppException(ErrorCode.MAX_RACES_REACHED);
        }

        if (round.isFinal()) {
            if (round.getRaces() != null && !round.getRaces().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_FINAL_ROUND_CONFIGURATION);
            }
        }

        if (raceRepository.existsByRound_RoundIdAndSequenceOrder(roundId, request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_RACE_SEQUENCE);
        }
        validateRaceScheduleConstraints(round, request.getStartTime(), endTime, null);

        User currentUser = getCurrentUser();

        Race race = raceMapper.toRace(request);
        race.setStatus(RoundStatus.SCHEDULING);
        race.setEndTime(endTime);
        race.setRound(round);
        race.setCreatedBy(currentUser);

        race.setPredictionOpenAt(request.getStartTime().minusHours(tournament.getPredictionCardOpenHoursBeforeFirstRace()));
        race.setPredictionCloseAt(request.getStartTime().minusMinutes(tournament.getPredictionCloseMinutesBefore()));

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    @Override
    @Transactional
    public RaceResponse update(UUID raceId, UpdateRaceRequest request) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        Round round = race.getRound();
        Tournament tournament = round.getTournament();
        boolean scheduleEditable = tournament.getStatus() == TournamentStatus.DRAFT
                || tournament.getPhase() == TournamentPhase.SCHEDULING;
        if (!scheduleEditable) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
        if (tournament.getBracketPlanStatus() == BracketPlanStatus.CONFIRMED
                || tournament.getBracketPlanStatus() == BracketPlanStatus.LOCKED) {
            validateOnlyRaceScheduleChanges(request);
        }

        if (request.getName() != null && !request.getName().equals(race.getName())
                && raceRepository.existsByRound_RoundIdAndName(round.getRoundId(), request.getName())) {
            throw new AppException(ErrorCode.RACE_NAME_ALREADY_EXISTS);
        }

        if (request.getSequenceOrder() != null && request.getSequenceOrder() != race.getSequenceOrder()
                && raceRepository.existsByRound_RoundIdAndSequenceOrder(round.getRoundId(), request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_RACE_SEQUENCE);
        }

        LocalDateTime newStartTime = request.getStartTime() != null ? request.getStartTime() : race.getStartTime();
        if (newStartTime == null || round.getStartDate() == null || round.getEndDate() == null) {
            throw new AppException(ErrorCode.RACE_SCHEDULE_INCOMPLETE);
        }
        LocalDateTime newEndTime = newStartTime.plusMinutes(round.getTournament().getDefaultRaceOperationalMinutes());

        if (!newStartTime.toLocalDate().equals(newEndTime.toLocalDate())) {
            throw new AppException(ErrorCode.INVALID_RACE_DATES);
        }

        if (newStartTime.isBefore(round.getStartDate()) || newEndTime.isAfter(round.getEndDate())) {
            throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
        }

        validateRaceScheduleConstraints(round, newStartTime, newEndTime, raceId);

        Integer oldSequence = race.getSequenceOrder();
        Integer newSequence = request.getSequenceOrder();

        raceMapper.updateRace(request, race);
        race.setEndTime(newEndTime);

        if (newSequence != null && !newSequence.equals(oldSequence)) {
            reorderRaces(round.getRoundId(), race.getRaceId(), newSequence);
        }

        race.setPredictionOpenAt(race.getStartTime().minusHours(tournament.getPredictionCardOpenHoursBeforeFirstRace()));
        race.setPredictionCloseAt(race.getStartTime().minusMinutes(tournament.getPredictionCloseMinutesBefore()));

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    @Override
    @Transactional
    public void delete(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getRound().getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
        if (race.getRound().getTournament().getBracketPlanStatus() != BracketPlanStatus.NOT_GENERATED) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
        }

        raceRepository.delete(race);
    }

    @Override
    @Transactional
    public RaceResponse publishSchedule(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        BracketPlanStatus bracketStatus = race.getRound().getTournament().getBracketPlanStatus();
        if (bracketStatus == BracketPlanStatus.CONFIRMED || bracketStatus == BracketPlanStatus.LOCKED) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
        }

        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }
        if (race.getSchedulePublishedAt() != null) {
            throw new AppException(ErrorCode.RACE_ALREADY_PUBLISHED);
        }

        int entryCount = raceEntryRepository.countByRace_RaceId(raceId);
        if (entryCount < race.getRound().getMinEntries()) {
            throw new AppException(ErrorCode.RACE_NOT_ENOUGH_ENTRIES);
        }

        int refereeCount = raceRefereeRepository.countByRace_RaceId(raceId);
        if (refereeCount < 1) {
            throw new AppException(ErrorCode.RACE_MISSING_REFEREES);
        }

        race.setStatus(RoundStatus.SCHEDULED);
        race.setSchedulePublishedAt(LocalDateTime.now());

        Tournament tournament = race.getRound().getTournament();
        List<Race> tournamentRaces = raceRepository.findByRound_Tournament_TournamentId(tournament.getTournamentId());
        LocalDateTime firstRaceStartTime = tournamentRaces.stream()
                .map(Race::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(race.getStartTime());

        LocalDateTime commonPredictionOpenAt = firstRaceStartTime.minusHours(tournament.getPredictionCardOpenHoursBeforeFirstRace());
        race.setPredictionOpenAt(commonPredictionOpenAt);
        race.setPredictionCloseAt(race.getStartTime().minusMinutes(tournament.getPredictionCloseMinutesBefore()));

        Race savedRace = raceRepository.save(race);
        markRoundScheduledIfAllRacesPublished(race.getRound());
        return raceMapper.toRaceResponse(savedRace);
    }

    private void markRoundScheduledIfAllRacesPublished(Round round) {
        List<Race> races = raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(round.getRoundId());
        if (races.isEmpty()) {
            return;
        }

        for (Race race : races) {
            if (race.getStatus() != RoundStatus.SCHEDULED
                    && race.getStatus() != RoundStatus.CANCELLED) {
                return;
            }
        }

        round.setStatus(RoundStatus.SCHEDULED);
        roundRepository.save(round);
    }

    private void reorderRaces(UUID roundId, UUID raceId, int newSequence) {
        List<Race> otherRaces = raceRepository.findByRound_RoundIdAndRaceIdNotOrderBySequenceOrderAsc(roundId, raceId);
        int seq = 1;
        for (Race r : otherRaces) {
            if (seq == newSequence) {
                seq++;
            }
            r.setSequenceOrder(seq);
            seq++;
        }
    }

    @Override
    public List<RaceResponse> getRacesByRoundId(UUID roundId) {
        return raceRepository.findByRound_RoundId(roundId)
                .stream()
                .map(raceMapper::toRaceResponse)
                .toList();
    }

    @Override
    @Transactional
    public RaceResponse startRace(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        User currentUser = getCurrentUser();
        validateRefereeCanOperateRace(race, currentUser);

        if (race.getStatus() != RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULED_STATUS);
        }

        LocalDateTime now = LocalDateTime.now();
        Tournament tournament = race.getRound().getTournament();
        LocalDateTime earliestStart = race.getStartTime().minusMinutes(tournament.getStartEarlyToleranceMinutes());
        LocalDateTime latestStart = race.getStartTime().plusMinutes(tournament.getStartLateToleranceMinutes());

        if (now.isBefore(earliestStart)) {
            throw new AppException(ErrorCode.RACE_START_TOO_EARLY);
        }
        if (now.isAfter(latestStart)) {
            throw new AppException(ErrorCode.RACE_START_WINDOW_EXPIRED);
        }

        // Lazy finalize entries if not yet finalized
        if (race.getInspectionFinalizedAt() == null) {
            finalizeRaceEntries(raceId);
        }

        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        int activeEntryCount = 0;
        for (RaceEntry entry : entries) {
            if (entry.getStatus() == RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE
                    || entry.getStatus() == RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE
                    || entry.getStatus() == RaceEntryStatus.SCRATCHED
                    || entry.getStatus() == RaceEntryStatus.DISQUALIFIED) {
                continue;
            }

            HorseInspection horseInspection = horseInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId())
                    .orElseThrow(() -> new AppException(ErrorCode.ENTRY_MISSING_HORSE_INSPECTION));

            if (horseInspection.getStatus() != InspectionStatus.CONFIRMED 
                    || horseInspection.getResult() != InspectionResult.PASS) {
                throw new AppException(ErrorCode.ENTRY_MISSING_HORSE_INSPECTION);
            }

            JockeyInspection jockeyInspection = jockeyInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId())
                    .orElseThrow(() -> new AppException(ErrorCode.ENTRY_MISSING_JOCKEY_INSPECTION));

            if (jockeyInspection.getStatus() != InspectionStatus.CONFIRMED 
                    || jockeyInspection.getResult() != InspectionResult.PASS) {
                throw new AppException(ErrorCode.ENTRY_MISSING_JOCKEY_INSPECTION);
            }

            if (horseInspection.getHandicapWeight() != null && horseInspection.getHandicapWeight() > 0) {
                if (!Boolean.TRUE.equals(horseInspection.getIsHandicapConfirmed())) {
                    throw new AppException(ErrorCode.ENTRY_HANDICAP_NOT_CONFIRMED);
                }
            }

            activeEntryCount++;
        }

        if (activeEntryCount < race.getRound().getMinEntries()) {
            throw new AppException(ErrorCode.RACE_NOT_ENOUGH_ACTIVE_ENTRIES);
        }

        race.setStatus(RoundStatus.ONGOING);
        race.setStartedAt(LocalDateTime.now());
        race.setStartedBy(currentUser);
        race.getRound().setStatus(RoundStatus.ONGOING);
        roundRepository.save(race.getRound());

        // Also release staff assignments since race started (from Phase 11 — Staff availability)
        // veterinarian and medical staff assigned to this race can be set to AVAILABLE or we just release the bận status.
        // Wait, how is staff availability represented? Let's check:
        // "Release staff khi race start, finish hoặc cancel.
        // Release staff khi assignment bị xóa/thay thế.
        // Bảo đảm transaction cập nhật assignment và status atomically."
        // Let's search for assignment / AVAILABLE to see what entities are updated.
        releaseStaffForRace(race.getRaceId());

        Race savedRace = raceRepository.save(race);
        notificationEventService.raceStarted(savedRace);
        return raceMapper.toRaceResponse(savedRace);
    }

    @Override
    @Transactional(readOnly = true)
    public RaceStartReadinessResponse getStartReadiness(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        User currentUser = getCurrentUser();
        validateRefereeCanOperateRace(race, currentUser);

        LocalDateTime now = LocalDateTime.now();
        Tournament tournament = race.getRound().getTournament();
        LocalDateTime earliestStart = race.getStartTime() == null ? null
                : race.getStartTime().minusMinutes(tournament.getStartEarlyToleranceMinutes());
        LocalDateTime latestStart = race.getStartTime() == null ? null
                : race.getStartTime().plusMinutes(tournament.getStartLateToleranceMinutes());

        List<String> raceBlockingReasons = new ArrayList<>();
        if (race.getStatus() != RoundStatus.SCHEDULED) {
            raceBlockingReasons.add("Cuộc đua không ở trạng thái đã lên lịch.");
        }
        if (earliestStart == null || latestStart == null) {
            raceBlockingReasons.add("Cuộc đua chưa có thời gian bắt đầu hợp lệ.");
        } else {
            if (now.isBefore(earliestStart)) {
                raceBlockingReasons.add("Chưa đến khung giờ được phép bắt đầu cuộc đua.");
            }
            if (now.isAfter(latestStart)) {
                raceBlockingReasons.add("Đã quá khung giờ được phép bắt đầu cuộc đua.");
            }
        }

        List<RaceEntryReadinessResponse> entryResponses = new ArrayList<>();
        int activeEntryCount = 0;
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        for (RaceEntry entry : entries) {
            List<String> entryReasons = new ArrayList<>();
            boolean active = isActiveRaceEntry(entry);
            HorseInspection horseInspection =
                    horseInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId()).orElse(null);
            JockeyInspection jockeyInspection =
                    jockeyInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId()).orElse(null);

            if (!active) {
                entryReasons.add("Lượt tham gia không còn ở trạng thái đủ điều kiện xuất phát.");
            } else {
                activeEntryCount++;
                if (horseInspection == null) {
                    entryReasons.add("Ngựa chưa có hồ sơ kiểm tra sức khỏe.");
                } else {
                    if (horseInspection.getStatus() != InspectionStatus.CONFIRMED) {
                        entryReasons.add("Kết quả kiểm tra sức khỏe ngựa chưa được xác nhận.");
                    }
                    if (horseInspection.getResult() != InspectionResult.PASS) {
                        entryReasons.add("Ngựa chưa đạt kiểm tra sức khỏe.");
                    }
                    if (horseInspection.getHandicapWeight() != null
                            && horseInspection.getHandicapWeight() > 0
                            && !Boolean.TRUE.equals(horseInspection.getIsHandicapConfirmed())) {
                        entryReasons.add("Mức cân handicap của ngựa chưa được xác nhận.");
                    }
                }
                if (jockeyInspection == null) {
                    entryReasons.add("Kỵ sĩ chưa có hồ sơ kiểm tra sức khỏe.");
                } else {
                    if (jockeyInspection.getStatus() != InspectionStatus.CONFIRMED) {
                        entryReasons.add("Kết quả kiểm tra sức khỏe kỵ sĩ chưa được xác nhận.");
                    }
                    if (jockeyInspection.getResult() != InspectionResult.PASS) {
                        entryReasons.add("Kỵ sĩ chưa đạt kiểm tra sức khỏe.");
                    }
                }
            }

            entryResponses.add(RaceEntryReadinessResponse.builder()
                    .entryId(entry.getEntryId())
                    .horseName(entry.getContract().getHorse().getName())
                    .jockeyName(entry.getContract().getJockey().getUser().getFullName())
                    .entryStatus(entry.getStatus())
                    .horseInspectionStatus(horseInspection == null ? null : horseInspection.getStatus())
                    .horseInspectionResult(horseInspection == null ? null : horseInspection.getResult())
                    .jockeyInspectionStatus(jockeyInspection == null ? null : jockeyInspection.getStatus())
                    .jockeyInspectionResult(jockeyInspection == null ? null : jockeyInspection.getResult())
                    .canRace(active && entryReasons.isEmpty())
                    .blockingReasons(entryReasons)
                    .build());
        }

        int minEntries = race.getRound().getMinEntries();
        if (activeEntryCount < minEntries) {
            raceBlockingReasons.add("Chưa đủ số lượt tham gia hoạt động tối thiểu để bắt đầu.");
        }
        for (RaceEntryReadinessResponse entry : entryResponses) {
            if (isActiveStatus(entry.getEntryStatus()) && !entry.isCanRace()) {
                raceBlockingReasons.add("Còn lượt tham gia chưa hoàn tất điều kiện kiểm tra sức khỏe.");
                break;
            }
        }

        return RaceStartReadinessResponse.builder()
                .raceId(race.getRaceId())
                .raceStatus(race.getStatus())
                .canStart(raceBlockingReasons.isEmpty())
                .inspectionFinalizedAt(race.getInspectionFinalizedAt())
                .startWindow(RaceStartWindowResponse.builder()
                        .earliestStart(earliestStart)
                        .latestStart(latestStart)
                        .serverNow(now)
                        .build())
                .activeEntryCount(activeEntryCount)
                .minEntries(minEntries)
                .blockingReasons(raceBlockingReasons)
                .entries(entryResponses)
                .build();
    }

    private boolean isActiveRaceEntry(RaceEntry entry) {
        return isActiveStatus(entry.getStatus());
    }

    private boolean isActiveStatus(RaceEntryStatus status) {
        return status != RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE
                && status != RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE
                && status != RaceEntryStatus.SCRATCHED
                && status != RaceEntryStatus.DISQUALIFIED;
    }

    private void validateRefereeCanOperateRace(Race race, User currentUser) {
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));
        boolean authorized = race.getRound().getHeadReferee() != null
                && race.getRound().getHeadReferee().getRefereeId().equals(referee.getRefereeId());
        if (!authorized) {
            authorized = raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                    race.getRaceId(), referee.getRefereeId());
        }
        if (!authorized) {
            throw new AppException(ErrorCode.REFEREE_NOT_ASSIGNED_TO_RACE);
        }
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateRaceScheduleConstraints(
            Round round, LocalDateTime newStartTime, LocalDateTime newEndTime, UUID excludeRaceId) {
        
        Tournament tournament = round.getTournament();
        
        // 1. Daily Limit
        LocalDateTime startOfDay = newStartTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = newStartTime.toLocalDate().atTime(23, 59, 59, 999999999);
        
        long count;
        if (excludeRaceId != null) {
            count = raceRepository.countByRound_Tournament_TournamentIdAndStartTimeBetweenAndStatusNotAndRaceIdNot(
                    tournament.getTournamentId(), startOfDay, endOfDay, RoundStatus.CANCELLED, excludeRaceId);
        } else {
            count = raceRepository.countByRound_Tournament_TournamentIdAndStartTimeBetweenAndStatusNot(
                    tournament.getTournamentId(), startOfDay, endOfDay, RoundStatus.CANCELLED);
        }
        
        if (count >= tournament.getMaxRacesPerDay()) {
            throw new AppException(ErrorCode.MAX_RACES_PER_DAY_EXCEEDED);
        }
        
        // 2. Operating Hours
        LocalTime startLocalTime = newStartTime.toLocalTime();
        LocalTime endLocalTime = newEndTime.toLocalTime();
        
        if (startLocalTime.isBefore(tournament.getRaceDayStartTime()) 
                || endLocalTime.isAfter(tournament.getRaceDayEndTime())) {
            throw new AppException(ErrorCode.RACE_OUTSIDE_OPERATING_HOURS);
        }
        
        // 3. Break Time
        if (Boolean.TRUE.equals(tournament.getApplyBreakTime())) {
            LocalTime breakStart = tournament.getBreakStartTime();
            LocalTime breakEnd = tournament.getBreakEndTime();
            if (breakStart != null && breakEnd != null) {
                if (!(endLocalTime.isBefore(breakStart) || endLocalTime.equals(breakStart) 
                        || startLocalTime.isAfter(breakEnd) || startLocalTime.equals(breakEnd))) {
                    throw new AppException(ErrorCode.RACE_OVERLAPS_BREAK);
                }
            }
        }
        
        // 4. Overlap & Minimum Interval
        List<Race> tournamentRaces = raceRepository.findByRound_Tournament_TournamentId(tournament.getTournamentId());
        long minInterval = tournament.getMinRaceIntervalMinutes();
        
        for (Race existing : tournamentRaces) {
            if (existing.getStatus() == RoundStatus.CANCELLED) {
                continue;
            }
            if (excludeRaceId != null && existing.getRaceId().equals(excludeRaceId)) {
                continue;
            }
            if (existing.getStartTime() == null || existing.getEndTime() == null) {
                continue;
            }
            
            LocalDateTime minStartAfterExisting = existing.getEndTime().plusMinutes(minInterval);
            LocalDateTime maxEndBeforeExisting = existing.getStartTime().minusMinutes(minInterval);
            
            if (!(newStartTime.isAfter(minStartAfterExisting) || newStartTime.isEqual(minStartAfterExisting)
                  || newEndTime.isBefore(maxEndBeforeExisting) || newEndTime.isEqual(maxEndBeforeExisting))) {
                throw new AppException(ErrorCode.RACE_SCHEDULE_CONFLICT);
            }
        }
    }

    @Override
    @Transactional
    public void finalizeRaceEntries(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getInspectionFinalizedAt() != null) {
            return;
        }

        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        for (RaceEntry entry : entries) {
            if (entry.getStatus() != RaceEntryStatus.CONFIRMED) {
                continue;
            }

            var horseOpt = horseInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId());
            var jockeyOpt = jockeyInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId());

            boolean hasHorsePass = horseOpt.isPresent() 
                    && horseOpt.get().getStatus() == InspectionStatus.CONFIRMED 
                    && horseOpt.get().getResult() == InspectionResult.PASS;

            boolean hasJockeyPass = jockeyOpt.isPresent() 
                    && jockeyOpt.get().getStatus() == InspectionStatus.CONFIRMED 
                    && jockeyOpt.get().getResult() == InspectionResult.PASS;

            if (!hasHorsePass) {
                entry.setStatus(RaceEntryStatus.SCRATCHED);
                entry.setScratchedReason("Missing or failed horse inspection at deadline");
                RaceEntry savedEntry = raceEntryRepository.save(entry);
                notificationEventService.entryScratched(savedEntry);
            } else if (!hasJockeyPass) {
                entry.setStatus(RaceEntryStatus.SCRATCHED);
                entry.setScratchedReason("Missing or failed jockey inspection at deadline");
                RaceEntry savedEntry = raceEntryRepository.save(entry);
                notificationEventService.entryScratched(savedEntry);
            }
        }

        race.setInspectionFinalizedAt(LocalDateTime.now());
        raceRepository.save(race);
    }

    private void releaseStaffForRace(UUID raceId) {
        var assignmentOpt = raceInspectionStaffAssignmentRepository.findByRace_RaceId(raceId);
        if (assignmentOpt.isPresent()) {
            RaceInspectionAssignment assignment = assignmentOpt.get();
            Veterinarian vet = assignment.getVeterinarian();
            if (vet.getStatus() == VetStatus.ASSIGNED) {
                vet.setStatus(VetStatus.AVAILABLE);
                veterinarianRepository.save(vet);
            }
            MedicalStaff med = assignment.getMedicalStaff();
            if (med.getStatus() == MedicalStaffStatus.ASSIGNED) {
                med.setStatus(MedicalStaffStatus.AVAILABLE);
                medicalStaffRepository.save(med);
            }
        }
    }

    @Override
    @Transactional
    public RaceResponse rescheduleRace(UUID raceId, com.swp391.horseracing.dto.tournament.request.RescheduleRaceRequest request) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULED_STATUS);
        }

        Round round = race.getRound();
        Tournament tournament = round.getTournament();

        LocalDateTime newEndTime = request.getNewStartTime().plusMinutes(tournament.getDefaultRaceOperationalMinutes());

        if (!request.getNewStartTime().toLocalDate().equals(newEndTime.toLocalDate())) {
            throw new AppException(ErrorCode.INVALID_RACE_DATES);
        }

        if (request.getNewStartTime().isBefore(round.getStartDate()) 
                || newEndTime.isAfter(round.getEndDate())) {
            throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
        }

        validateRaceScheduleConstraints(round, request.getNewStartTime(), newEndTime, raceId);
        validateRescheduleConflictsInternal(race, request.getNewStartTime(), newEndTime);

        LocalDateTime oldStartTime = race.getStartTime();
        race.setStartTime(request.getNewStartTime());
        race.setEndTime(newEndTime);
        race.setRescheduledAt(LocalDateTime.now());
        race.setRescheduleReason(request.getReason());

        // Recalculate prediction open / close
        LocalDateTime now = LocalDateTime.now();
        if (race.getPredictionOpenAt() == null || race.getPredictionOpenAt().isAfter(now)) {
            race.setPredictionOpenAt(request.getNewStartTime().minusHours(tournament.getPredictionCardOpenHoursBeforeFirstRace()));
        }
        race.setPredictionCloseAt(request.getNewStartTime().minusMinutes(tournament.getPredictionCloseMinutesBefore()));

        // Since it's rescheduled, clear finalized time and delete existing inspections to allow re-inspection
        race.setInspectionFinalizedAt(null);

        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        for (RaceEntry entry : entries) {
            horseInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId())
                    .ifPresent(ins -> horseInspectionRepository.delete(ins));
            jockeyInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId())
                    .ifPresent(ins -> jockeyInspectionRepository.delete(ins));
            entry.setStatus(RaceEntryStatus.CONFIRMED);
            raceEntryRepository.save(entry);
        }

        Race savedRace = raceRepository.save(race);
        notificationEventService.raceRescheduled(savedRace, oldStartTime, request.getReason());
        return raceMapper.toRaceResponse(savedRace);
    }

    @Override
    @Transactional
    public void cancelRace(UUID raceId, com.swp391.horseracing.dto.tournament.request.CancelRaceRequest request) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() == RoundStatus.CANCELLED) {
            return; // Already cancelled
        }

        if (race.getStatus() == RoundStatus.FINISHED || race.getStatus() == RoundStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        race.setStatus(RoundStatus.CANCELLED);
        race.setCancelledAt(LocalDateTime.now());
        race.setCancellationReason(request.getReason());
        raceRepository.save(race);

        // Release veterinarian/medical staff assigned to race
        releaseStaffForRace(raceId);

        // Void all predictions for this race
        predictionService.voidAllPredictionsForRace(raceId, request.getReason());
        notificationEventService.raceCancelled(race, request.getReason());
    }

    private void validateRescheduleConflictsInternal(Race race, LocalDateTime newStartTime, LocalDateTime newEndTime) {
        Tournament tournament = race.getRound().getTournament();
        long minInterval = tournament.getMinRaceIntervalMinutes();
        UUID raceId = race.getRaceId();

        // 1. Verify sequence order dependency (earlier rounds cannot start after subsequent rounds have already started)
        List<Race> otherRoundRaces = raceRepository.findByRound_Tournament_TournamentId(tournament.getTournamentId());
        for (Race other : otherRoundRaces) {
            if (other.getRound().getSequenceOrder() > race.getRound().getSequenceOrder() && other.getStartedAt() != null) {
                if (newStartTime.isAfter(other.getStartTime()) || newStartTime.isEqual(other.getStartTime())) {
                    throw new AppException(ErrorCode.INVALID_RACE_DATES);
                }
            }
        }

        // 2. Fetch race entries
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        for (RaceEntry entry : entries) {
            UUID horseId = entry.getContract().getHorse().getHorseId();
            UUID jockeyId = entry.getContract().getJockey().getJockeyId();

            // Horse conflict & rest time (60 min)
            List<RaceEntry> horseEntries = raceEntryRepository.findByContract_Horse_HorseId(horseId);
            for (RaceEntry he : horseEntries) {
                Race otherRace = he.getRace();
                if (otherRace.getRaceId().equals(raceId) || otherRace.getStatus() == RoundStatus.CANCELLED) {
                    continue;
                }
                if (otherRace.getStartTime() == null || otherRace.getEndTime() == null) {
                    continue;
                }
                long restThreshold = 60; // 60 minutes rest time
                LocalDateTime otherStart = otherRace.getStartTime();
                LocalDateTime otherEnd = otherRace.getEndTime();
                
                LocalDateTime earliestStartAllowed = otherEnd.plusMinutes(restThreshold);
                LocalDateTime latestEndAllowed = otherStart.minusMinutes(restThreshold);
                
                if (!(newStartTime.isAfter(earliestStartAllowed) || newStartTime.isEqual(earliestStartAllowed)
                        || newEndTime.isBefore(latestEndAllowed) || newEndTime.isEqual(latestEndAllowed))) {
                    throw new AppException(ErrorCode.RACE_SCHEDULE_CONFLICT);
                }
            }

            // Jockey conflict
            List<RaceEntry> jockeyEntries = raceEntryRepository.findByContract_Jockey_JockeyId(jockeyId);
            for (RaceEntry je : jockeyEntries) {
                Race otherRace = je.getRace();
                if (otherRace.getRaceId().equals(raceId) || otherRace.getStatus() == RoundStatus.CANCELLED) {
                    continue;
                }
                if (otherRace.getStartTime() == null || otherRace.getEndTime() == null) {
                    continue;
                }
                LocalDateTime otherStart = otherRace.getStartTime();
                LocalDateTime otherEnd = otherRace.getEndTime();
                
                LocalDateTime earliestStartAllowed = otherEnd.plusMinutes(minInterval);
                LocalDateTime latestEndAllowed = otherStart.minusMinutes(minInterval);
                
                if (!(newStartTime.isAfter(earliestStartAllowed) || newStartTime.isEqual(earliestStartAllowed)
                        || newEndTime.isBefore(latestEndAllowed) || newEndTime.isEqual(latestEndAllowed))) {
                    throw new AppException(ErrorCode.RACE_SCHEDULE_CONFLICT);
                }
            }
        }

        // 3. Referee conflict
        List<RaceReferee> referees = raceRefereeRepository.findByRace_RaceId(raceId);
        for (RaceReferee rr : referees) {
            UUID refereeId = rr.getReferee().getRefereeId();
            List<RaceReferee> otherAssignments = raceRefereeRepository.findByReferee_RefereeId(refereeId);
            for (RaceReferee oa : otherAssignments) {
                Race otherRace = oa.getRace();
                if (otherRace.getRaceId().equals(raceId) || otherRace.getStatus() == RoundStatus.CANCELLED) {
                    continue;
                }
                if (otherRace.getStartTime() == null || otherRace.getEndTime() == null) {
                    continue;
                }
                LocalDateTime otherStart = otherRace.getStartTime();
                LocalDateTime otherEnd = otherRace.getEndTime();
                
                LocalDateTime earliestStartAllowed = otherEnd.plusMinutes(minInterval);
                LocalDateTime latestEndAllowed = otherStart.minusMinutes(minInterval);
                
                if (!(newStartTime.isAfter(earliestStartAllowed) || newStartTime.isEqual(earliestStartAllowed)
                        || newEndTime.isBefore(latestEndAllowed) || newEndTime.isEqual(latestEndAllowed))) {
                    throw new AppException(ErrorCode.RACE_SCHEDULE_CONFLICT);
                }
            }
        }

        // 4. Veterinarian & Medical Staff conflict
        var assignmentOpt = raceInspectionStaffAssignmentRepository.findByRace_RaceId(raceId);
        if (assignmentOpt.isPresent()) {
            RaceInspectionAssignment assign = assignmentOpt.get();
            int openMin = tournament.getInspectionOpenMinutesBefore();
            int closeMin = tournament.getInspectionCloseMinutesBefore();
            LocalDateTime thisWindowStart = newStartTime.minusMinutes(openMin);
            LocalDateTime thisWindowEnd = newStartTime.minusMinutes(closeMin);

            if (assign.getVeterinarian() != null) {
                UUID vetId = assign.getVeterinarian().getVetId();
                List<RaceInspectionAssignment> otherAssigns = raceInspectionStaffAssignmentRepository.findByVeterinarian_VetId(vetId);
                for (RaceInspectionAssignment oa : otherAssigns) {
                    Race otherRace = oa.getRace();
                    if (otherRace.getRaceId().equals(raceId) || otherRace.getStatus() == RoundStatus.CANCELLED) {
                        continue;
                    }
                    if (otherRace.getStartTime() == null) {
                        continue;
                    }
                    LocalDateTime otherWindowStart = otherRace.getStartTime().minusMinutes(openMin);
                    LocalDateTime otherWindowEnd = otherRace.getStartTime().minusMinutes(closeMin);
                    
                    if (!(thisWindowEnd.isBefore(otherWindowStart) || thisWindowEnd.isEqual(otherWindowStart)
                            || thisWindowStart.isAfter(otherWindowEnd) || thisWindowStart.isEqual(otherWindowEnd))) {
                        throw new AppException(ErrorCode.RACE_SCHEDULE_CONFLICT);
                    }
                }
            }
            
            if (assign.getMedicalStaff() != null) {
                UUID medId = assign.getMedicalStaff().getMedStaffId();
                List<RaceInspectionAssignment> otherAssigns = raceInspectionStaffAssignmentRepository.findByMedicalStaff_MedStaffId(medId);
                for (RaceInspectionAssignment oa : otherAssigns) {
                    Race otherRace = oa.getRace();
                    if (otherRace.getRaceId().equals(raceId) || otherRace.getStatus() == RoundStatus.CANCELLED) {
                        continue;
                    }
                    if (otherRace.getStartTime() == null) {
                        continue;
                    }
                    LocalDateTime otherWindowStart = otherRace.getStartTime().minusMinutes(openMin);
                    LocalDateTime otherWindowEnd = otherRace.getStartTime().minusMinutes(closeMin);
                    
                    if (!(thisWindowEnd.isBefore(otherWindowStart) || thisWindowEnd.isEqual(otherWindowStart)
                            || thisWindowStart.isAfter(otherWindowEnd) || thisWindowStart.isEqual(otherWindowEnd))) {
                        throw new AppException(ErrorCode.RACE_SCHEDULE_CONFLICT);
                    }
                }
            }
        }
    }

    @Override
    public List<LocalDateTime> getRescheduleProposals(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        Round round = race.getRound();
        Tournament tournament = round.getTournament();
        
        List<LocalDateTime> proposals = new ArrayList<>();
        
        LocalDate searchDate = LocalDate.now();
        LocalDate competitionStartDate = tournament.getCompetitionStartAt().toLocalDate();
        if (searchDate.isBefore(competitionStartDate)) {
            searchDate = competitionStartDate;
        }
        
        LocalDate endDate = tournament.getEndDate();
        
        int daysChecked = 0;
        while (!searchDate.isAfter(endDate) && proposals.size() < 10 && daysChecked < 30) {
            LocalDateTime startOfDay = searchDate.atStartOfDay();
            LocalDateTime endOfDay = searchDate.atTime(23, 59, 59, 999999999);
            
            long count = raceRepository.countByRound_Tournament_TournamentIdAndStartTimeBetweenAndStatusNotAndRaceIdNot(
                    tournament.getTournamentId(), startOfDay, endOfDay, RoundStatus.CANCELLED, raceId);
            
            if (count < tournament.getMaxRacesPerDay()) {
                LocalTime raceTime = tournament.getRaceDayStartTime();
                LocalTime dayEndTime = tournament.getRaceDayEndTime();
                
                while (raceTime.isBefore(dayEndTime)) {
                    LocalDateTime startCandidate = LocalDateTime.of(searchDate, raceTime);
                    LocalDateTime endCandidate = startCandidate.plusMinutes(tournament.getDefaultRaceOperationalMinutes());
                    
                    if (endCandidate.toLocalTime().isAfter(dayEndTime)) {
                        break;
                    }
                    
                    if (startCandidate.isAfter(LocalDateTime.now())) {
                        try {
                            validateRaceScheduleConstraints(round, startCandidate, endCandidate, raceId);
                            validateRescheduleConflictsInternal(race, startCandidate, endCandidate);
                            proposals.add(startCandidate);
                        } catch (AppException e) {
                            // ignore and try next candidate
                        }
                    }
                    
                    raceTime = raceTime.plusMinutes(30);
                }
            }
            searchDate = searchDate.plusDays(1);
            daysChecked++;
        }
        
        return proposals;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRoundScheduleForPublication(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));
        if (round.getStartDate() == null || round.getEndDate() == null) {
            throw new AppException(ErrorCode.RACE_SCHEDULE_INCOMPLETE);
        }

        List<Race> races = raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(roundId);
        if (races.isEmpty()) {
            throw new AppException(ErrorCode.RACE_STRUCTURE_MISMATCH);
        }
        int durationMinutes = round.getTournament().getDefaultRaceOperationalMinutes();
        for (Race race : races) {
            if (race.getStartTime() == null || race.getEndTime() == null) {
                throw new AppException(ErrorCode.RACE_SCHEDULE_INCOMPLETE);
            }
            LocalDateTime requiredEndTime = race.getStartTime().plusMinutes(durationMinutes);
            if (!requiredEndTime.equals(race.getEndTime())) {
                throw new AppException(ErrorCode.INVALID_RACE_DATES);
            }
            if (race.getStartTime().isBefore(round.getStartDate())
                    || race.getEndTime().isAfter(round.getEndDate())) {
                throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
            }
            validateRaceScheduleConstraints(round, race.getStartTime(), race.getEndTime(), race.getRaceId());
            validateRescheduleConflictsInternal(race, race.getStartTime(), race.getEndTime());
        }
    }

    private void validateOnlyRaceScheduleChanges(UpdateRaceRequest request) {
        if (request.getName() != null
                || request.getTrackCondition() != null
                || request.getDistance() != null
                || request.getSequenceOrder() != null) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
        }
    }
}
