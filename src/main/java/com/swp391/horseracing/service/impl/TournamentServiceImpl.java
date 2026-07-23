package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.config.HorseRatingProperties;
import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.request.TournamentRatingConfigRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentRatingConfigResponse;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TournamentMapper;
import com.swp391.horseracing.policy.TournamentTimelinePolicy;
import com.swp391.horseracing.repository.PhaseTimingConfigRepository;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentEligibilityRepository;
import com.swp391.horseracing.repository.TournamentPhaseConfigRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.service.CloudinaryService;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import com.swp391.horseracing.service.TournamentService;
import com.swp391.horseracing.service.RaceService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    TournamentRepository tournamentRepository;
    UserRepository userRepository;
    TournamentMapper tournamentMapper;
    PrizeStructureRepository prizeStructureRepository;
    TournamentEligibilityRepository eligibilityRepository;
    RoundRepository roundRepository;
    RaceRepository raceRepository;
    RaceRefereeRepository raceRefereeRepository;
    HorseTournamentRegistrationRepository horseRegistrationRepository;
    InvoiceRepository invoiceRepository;
    RaceEntryRepository raceEntryRepository;
    InvoiceService invoiceService;
    BusinessNotificationEventService notificationEventService;
    RaceService raceService;
    CloudinaryService cloudinaryService;
    PhaseTimingConfigRepository phaseTimingConfigRepository;
    TournamentPhaseConfigRepository tournamentPhaseConfigRepository;
    HorseRatingProperties horseRatingProperties;

    @Override
    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        LocalDate today = LocalDate.now();
        if (!today.equals(request.getStartDate())) {
            throw new AppException(ErrorCode.TOURNAMENT_START_DATE_MUST_BE_TODAY);
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
        }

        Integer maxEntries = request.getMaxApprovedEntries();
        validateMaxApprovedEntries(maxEntries);

        if (tournamentRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TOURNAMENT_NAME_EXISTS);
        }

        validateSchedulingAndTimeline(
                request.getInspectionOpenMinutesBefore(),
                request.getInspectionCloseMinutesBefore(),
                request.getPredictionCloseMinutesBefore(),
                request.getMinRaceIntervalMinutes(),
                request.getStartEarlyToleranceMinutes(),
                request.getStartLateToleranceMinutes(),
                request.getDefaultRaceOperationalMinutes(),
                request.getPredictionCardOpenHoursBeforeFirstRace(),
                request.getRaceDayStartTime(),
                request.getRaceDayEndTime(),
                request.getApplyBreakTime(),
                request.getBreakStartTime(),
                request.getBreakEndTime()
        );

        if (request.getMinHorseAge() >= request.getMaxHorseAge()) {
            throw new AppException(ErrorCode.INVALID_HORSE_AGE_RANGE);
        }

        validateTimingOrder(
                request.getRegistrationOpenAt(),
                request.getRegistrationCloseAt(),
                request.getReviewDeadlineAt(),
                request.getJockeyMatchingDeadlineAt(),
                request.getSchedulingDeadlineAt()
        );
        Map<String, Integer> phaseConfigs = resolvePhaseConfigs(request.getPhaseConfigs(), maxEntries);
        validateTournamentTimeline(
                request.getStartDate(),
                request.getRegistrationOpenAt(),
                request.getRegistrationCloseAt(),
                request.getReviewDeadlineAt(),
                request.getJockeyMatchingDeadlineAt(),
                request.getSchedulingDeadlineAt(),
                phaseConfigs,
                true
        );

        validateHandicapSettings(request.getHandicapEnabled(), request.getTopWeightLbs(), request.getMinWeightLbs(), request.getEquipmentWeightKg());

        User currentUser = getCurrentUser();

        Tournament tournament = tournamentMapper.toTournament(request);
        applyInitialRatingConfig(tournament, request.getRatingConfig());
        
        if (!tournament.isHandicapEnabled()) {
            tournament.setTopWeightLbs(0);
            tournament.setMinWeightLbs(0);
            tournament.setEquipmentWeightKg(0.0);
        }

        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPhase(TournamentPhase.DRAFT);
        tournament.setMaxApprovedEntries(maxEntries);
        tournament.setMaxApprovedHorses(maxEntries);
        tournament.setMaxApprovedJockeys(maxEntries);
        tournament.setCreatedBy(currentUser);
        tournament.setCreatedAt(LocalDateTime.now());
        tournament.setCompetitionStartAt(TournamentTimelinePolicy.competitionStartAt(
                tournament.getSchedulingDeadlineAt(), tournament.getRaceDayStartTime(),
                phaseConfigs.get("PRE_RACE_BUFFER")));

        Tournament saved = tournamentRepository.save(tournament);
        savePhaseConfigs(saved.getTournamentId(), phaseConfigs);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TournamentResponse update(UUID id, UpdateTournamentRequest request) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (request.getName() != null && !request.getName().equals(tournament.getName())
                && tournamentRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TOURNAMENT_NAME_EXISTS);
        }

        if (request.getStartDate() != null
                && !request.getStartDate().equals(tournament.getStartDate())) {
            throw new AppException(ErrorCode.TOURNAMENT_START_DATE_IMMUTABLE);
        }
        if (request.getEndDate() != null
                && request.getEndDate().isBefore(tournament.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
        }

        if (request.getMinHorseAge() != null && request.getMaxHorseAge() != null
                && request.getMinHorseAge() >= request.getMaxHorseAge()) {
            throw new AppException(ErrorCode.INVALID_HORSE_AGE_RANGE);
        }
        if (request.getMinHorseAge() != null && request.getMaxHorseAge() == null
                && request.getMinHorseAge() >= tournament.getMaxHorseAge()) {
            throw new AppException(ErrorCode.INVALID_HORSE_AGE_RANGE);
        }
        if (request.getMinHorseAge() == null && request.getMaxHorseAge() != null
                && tournament.getMinHorseAge() >= request.getMaxHorseAge()) {
            throw new AppException(ErrorCode.INVALID_HORSE_AGE_RANGE);
        }

        Integer insOpen = request.getInspectionOpenMinutesBefore() != null ? request.getInspectionOpenMinutesBefore() : tournament.getInspectionOpenMinutesBefore();
        Integer insClose = request.getInspectionCloseMinutesBefore() != null ? request.getInspectionCloseMinutesBefore() : tournament.getInspectionCloseMinutesBefore();
        Integer predClose = request.getPredictionCloseMinutesBefore() != null ? request.getPredictionCloseMinutesBefore() : tournament.getPredictionCloseMinutesBefore();
        Integer minInterval = request.getMinRaceIntervalMinutes() != null ? request.getMinRaceIntervalMinutes() : tournament.getMinRaceIntervalMinutes();
        Integer startEarly = request.getStartEarlyToleranceMinutes() != null ? request.getStartEarlyToleranceMinutes() : tournament.getStartEarlyToleranceMinutes();
        Integer startLate = request.getStartLateToleranceMinutes() != null ? request.getStartLateToleranceMinutes() : tournament.getStartLateToleranceMinutes();
        Integer operational = request.getDefaultRaceOperationalMinutes() != null ? request.getDefaultRaceOperationalMinutes() : tournament.getDefaultRaceOperationalMinutes();
        Integer openHours = request.getPredictionCardOpenHoursBeforeFirstRace() != null ? request.getPredictionCardOpenHoursBeforeFirstRace() : tournament.getPredictionCardOpenHoursBeforeFirstRace();

        LocalTime dayStart = request.getRaceDayStartTime() != null ? request.getRaceDayStartTime() : tournament.getRaceDayStartTime();
        LocalTime dayEnd = request.getRaceDayEndTime() != null ? request.getRaceDayEndTime() : tournament.getRaceDayEndTime();
        Boolean applyBreak = request.getApplyBreakTime() != null ? request.getApplyBreakTime() : tournament.getApplyBreakTime();
        LocalTime breakStart = request.getBreakStartTime() != null ? request.getBreakStartTime() : (applyBreak ? tournament.getBreakStartTime() : null);
        LocalTime breakEnd = request.getBreakEndTime() != null ? request.getBreakEndTime() : (applyBreak ? tournament.getBreakEndTime() : null);

        validateSchedulingAndTimeline(insOpen, insClose, predClose, minInterval, startEarly, startLate, operational, openHours, dayStart, dayEnd, applyBreak, breakStart, breakEnd);

        LocalDateTime regOpen = request.getRegistrationOpenAt() != null
                ? request.getRegistrationOpenAt() : tournament.getRegistrationOpenAt();
        LocalDateTime regClose = request.getRegistrationCloseAt() != null
                ? request.getRegistrationCloseAt() : tournament.getRegistrationCloseAt();
        LocalDateTime reviewAt = request.getReviewDeadlineAt() != null
                ? request.getReviewDeadlineAt() : tournament.getReviewDeadlineAt();
        LocalDateTime matchAt = request.getJockeyMatchingDeadlineAt() != null
                ? request.getJockeyMatchingDeadlineAt() : tournament.getJockeyMatchingDeadlineAt();
        LocalDateTime schedAt = request.getSchedulingDeadlineAt() != null
                ? request.getSchedulingDeadlineAt() : tournament.getSchedulingDeadlineAt();
        validateTimingOrder(regOpen, regClose, reviewAt, matchAt, schedAt);

        Boolean newHandicapEnabled = request.getHandicapEnabled() != null
                ? request.getHandicapEnabled() : tournament.isHandicapEnabled();

        Integer topWeightLbs = request.getTopWeightLbs();
        if (topWeightLbs == null && newHandicapEnabled) {
            topWeightLbs = tournament.getTopWeightLbs();
        }

        Integer minWeightLbs = request.getMinWeightLbs();
        if (minWeightLbs == null && newHandicapEnabled) {
            minWeightLbs = tournament.getMinWeightLbs();
        }

        Double equipmentWeightKg = request.getEquipmentWeightKg();
        if (equipmentWeightKg == null && newHandicapEnabled) {
            equipmentWeightKg = tournament.getEquipmentWeightKg();
        }

        validateHandicapSettings(newHandicapEnabled, topWeightLbs, minWeightLbs, equipmentWeightKg);

        if (request.getMaxApprovedEntries() != null) {
            validateMaxApprovedEntries(request.getMaxApprovedEntries());
        }

        Integer effectiveMaxEntries = request.getMaxApprovedEntries() != null
                ? request.getMaxApprovedEntries() : tournament.getMaxApprovedEntries();
        Map<String, Integer> phaseConfigs = resolvePhaseConfigs(request.getPhaseConfigs(), effectiveMaxEntries);
        validateTournamentTimeline(
                tournament.getStartDate(),
                regOpen,
                regClose,
                reviewAt,
                matchAt,
                schedAt,
                phaseConfigs,
                false
        );
        applyRatingConfigUpdate(tournament, request.getRatingConfig());
        tournamentMapper.updateTournament(request, tournament);
        tournament.setMaxApprovedHorses(tournament.getMaxApprovedEntries());
        tournament.setCompetitionStartAt(TournamentTimelinePolicy.competitionStartAt(
                tournament.getSchedulingDeadlineAt(), tournament.getRaceDayStartTime(),
                phaseConfigs.get("PRE_RACE_BUFFER")));

        if (!tournament.isHandicapEnabled()) {
            tournament.setTopWeightLbs(0);
            tournament.setMinWeightLbs(0);
            tournament.setEquipmentWeightKg(0.0);
        }

        Tournament saved = tournamentRepository.save(tournament);
        if (request.getPhaseConfigs() != null) {
            savePhaseConfigs(saved.getTournamentId(), phaseConfigs);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        tournamentRepository.delete(tournament);
    }

    @Override
    @Transactional
    public TournamentResponse publish(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (prizeStructureRepository.findByTournament_TournamentId(id).isEmpty()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_PRIZE);
        }

        if (eligibilityRepository.findByTournament_TournamentId(id).isEmpty()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_ELIGIBILITY);
        }

        LocalDateTime publishedAt = LocalDateTime.now();
        tournament.setPublishedAt(publishedAt);
        tournament.setRatingPolicyLockedAt(publishedAt);

        activatePrizeStructures(id);

        setPhaseAndStatus(tournament, TournamentPhase.REGISTRATION_OPEN);
        Tournament savedTournament = tournamentRepository.save(tournament);
        notificationEventService.tournamentPublished(savedTournament);
        return toResponse(savedTournament);
    }

    @Override
    @Transactional
    public TournamentResponse completeReview(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        validatePhase(tournament, TournamentPhase.REGISTRATION_REVIEW);
        setPhaseAndStatus(tournament, TournamentPhase.JOCKEY_MATCHING);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Override
    @Transactional
    public TournamentResponse completeMatching(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        validatePhase(tournament, TournamentPhase.JOCKEY_MATCHING);

        setPhaseAndStatus(tournament, TournamentPhase.SCHEDULING);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Override
    @Transactional
    public TournamentResponse publishSchedule(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        validatePhase(tournament, TournamentPhase.SCHEDULING);

        List<Round> orderedRounds = roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(id);
        if (orderedRounds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }

        Round activeRound = null;
        for (Round round : orderedRounds) {
            List<Race> roundRaces = raceRepository.findByRound_RoundId(round.getRoundId());
            for (Race race : roundRaces) {
                if (race.getStatus() == RoundStatus.SCHEDULING) {
                    activeRound = round;
                    break;
                }
            }
            if (activeRound != null) {
                break;
            }
        }

        if (activeRound == null) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }

        if (activeRound.getHeadReferee() == null) {
            throw new AppException(ErrorCode.ROUND_MISSING_HEAD_REFEREE);
        }
        if (activeRound.getHeadReferee().getStatus()
                == com.swp391.horseracing.enums.RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.REFEREE_NOT_AVAILABLE);
        }
        if (raceRefereeRepository.existsByRace_Round_RoundIdAndReferee_RefereeId(
                activeRound.getRoundId(), activeRound.getHeadReferee().getRefereeId())) {
            throw new AppException(ErrorCode.REFEREE_ROLE_CONFLICT_IN_ROUND);
        }

        List<Race> racesToPublish = raceRepository.findByRound_RoundId(activeRound.getRoundId());
        if (racesToPublish.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }
        raceService.validateRoundScheduleForPublication(activeRound.getRoundId());

        for (Race race : racesToPublish) {
            if (race.getStatus() != RoundStatus.SCHEDULING && race.getStatus() != RoundStatus.SCHEDULED) {
                throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
            }
            List<RaceEntry> raceEntries = raceEntryRepository
                    .findByRace_RaceIdOrderByCreatedAtAsc(race.getRaceId());
            int entryCount = raceEntries.size();
            if (entryCount < race.getRound().getMinEntries()) {
                throw new AppException(ErrorCode.RACE_NOT_ENOUGH_ENTRIES);
            }
            Set<Integer> assignedLanes = new HashSet<>();
            for (RaceEntry entry : raceEntries) {
                Integer laneNumber = entry.getLaneNumber();
                if (laneNumber == null
                        || laneNumber < 1
                        || laneNumber > race.getRound().getMaxEntries()
                        || !assignedLanes.add(laneNumber)) {
                    throw new AppException(ErrorCode.RACE_LANES_INCOMPLETE);
                }
            }
            int refereeCount = raceRefereeRepository.countByRace_RaceId(race.getRaceId());
            if (refereeCount != 1) {
                throw new AppException(ErrorCode.RACE_REQUIRES_EXACTLY_ONE_REFEREE);
            }
        }

        LocalDateTime firstRaceStartTime = null;
        for (Race race : racesToPublish) {
            if (firstRaceStartTime == null || race.getStartTime().isBefore(firstRaceStartTime)) {
                firstRaceStartTime = race.getStartTime();
            }
        }
        if (firstRaceStartTime == null) {
            throw new AppException(ErrorCode.RACE_SCHEDULE_INCOMPLETE);
        }

        LocalDateTime commonPredictionOpenAt = firstRaceStartTime.minusHours(tournament.getPredictionCardOpenHoursBeforeFirstRace());

        for (Race race : racesToPublish) {
            race.setStatus(RoundStatus.SCHEDULED);
            race.setSchedulePublishedAt(LocalDateTime.now());
            race.setPredictionOpenAt(commonPredictionOpenAt);
            race.setPredictionCloseAt(race.getStartTime().minusMinutes(tournament.getPredictionCloseMinutesBefore()));
            raceRepository.save(race);
        }

        activeRound.setStatus(RoundStatus.SCHEDULED);
        roundRepository.save(activeRound);

        tournament.setCurrentRoundName(activeRound.getRoundName());
        setPhaseAndStatus(tournament, TournamentPhase.RACING);

        Tournament savedTournament = tournamentRepository.save(tournament);
        notificationEventService.schedulePublished(savedTournament);
        return toResponse(savedTournament);
    }

    @Override
    @Transactional
    public TournamentResponse publishResults(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        validatePhase(tournament, TournamentPhase.RESULT_PENDING);
        setPhaseAndStatus(tournament, TournamentPhase.RESULT_PUBLISHED);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Override
    @Transactional
    public TournamentResponse closeRegistration(UUID id) {
        Tournament tournament = tournamentRepository.findForUpdateByTournamentId(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        if (tournament.getPhase() != TournamentPhase.REGISTRATION_OPEN) {
            throw new AppException(ErrorCode.REGISTRATION_ALREADY_CLOSED);
        }

        LocalDateTime now = LocalDateTime.now();
        User admin = getCurrentUser();
        List<HorseTournamentRegistration> unpaidRegistrations = horseRegistrationRepository
                .findForUpdateByTournamentIdAndStatus(id, RegistrationStatus.PENDING_PAYMENT);

        for (HorseTournamentRegistration registration : unpaidRegistrations) {
            java.util.Optional<Invoice> invoice = invoiceRepository
                    .findByHorseTournamentRegistration_HorseRegistrationId(registration.getHorseRegistrationId());
            if (invoice.isPresent() && invoice.get().getStatus() == InvoiceStatus.UNPAID) {
                invoiceService.cancelInvoice(invoice.get().getInvoiceId());
            }
            registration.setStatus(RegistrationStatus.REJECTED);
            registration.setReviewedBy(admin);
            registration.setReviewedAt(now);
            registration.setRejectedReason("Registration closed before payment was completed");
            horseRegistrationRepository.save(registration);
        }

        tournament.setRegistrationCloseAt(now);
        setPhaseAndStatus(tournament, TournamentPhase.REGISTRATION_REVIEW);
        return toResponse(tournamentRepository.save(tournament));
    }

    private void setPhaseAndStatus(Tournament tournament, TournamentPhase phase) {
        tournament.setPhase(phase);
        tournament.setStatus(matchStatus(phase));
    }

    private void activatePrizeStructures(UUID tournamentId) {
        List<PrizeStructure> prizes = prizeStructureRepository.findByTournament_TournamentId(tournamentId);
        for (PrizeStructure prize : prizes) {
            prize.setActive(true);
        }
        prizeStructureRepository.saveAll(prizes);
    }

    private TournamentStatus matchStatus(TournamentPhase phase) {
        return switch (phase) {
            case DRAFT -> TournamentStatus.DRAFT;
            case REGISTRATION_OPEN, REGISTRATION_REVIEW -> TournamentStatus.OPEN;
            case JOCKEY_MATCHING, SCHEDULING, RACING, RESULT_PENDING -> TournamentStatus.ONGOING;
            case RESULT_PUBLISHED, FINISHED -> TournamentStatus.FINISHED;
        };
    }

    private void validatePhase(Tournament tournament, TournamentPhase expected) {
        if (tournament.getPhase() != expected) {
            throw new AppException(ErrorCode.INVALID_PHASE_TRANSITION);
        }
    }

    private void validateTimingOrder(LocalDateTime openAt, LocalDateTime closeAt,
                                      LocalDateTime reviewAt, LocalDateTime matchingAt,
                                      LocalDateTime schedulingAt) {
        if (!(openAt.isBefore(closeAt)
                && closeAt.isBefore(reviewAt)
                && reviewAt.isBefore(matchingAt)
                && matchingAt.isBefore(schedulingAt))) {
            throw new AppException(ErrorCode.INVALID_TIMING_ORDER);
        }
    }

    private void validateTournamentTimeline(LocalDate startDate,
                                            LocalDateTime registrationOpenAt,
                                            LocalDateTime registrationCloseAt,
                                            LocalDateTime reviewDeadlineAt,
                                            LocalDateTime jockeyMatchingDeadlineAt,
                                            LocalDateTime schedulingDeadlineAt,
                                            Map<String, Integer> phaseDays,
                                            boolean validateOpenTimeAgainstNow) {
        if (registrationOpenAt.toLocalDate().isBefore(startDate)) {
            throw new AppException(ErrorCode.REGISTRATION_OPEN_TIME_IN_PAST);
        }
        if (validateOpenTimeAgainstNow) {
            LocalDateTime currentMinute = LocalDateTime.now().withSecond(0).withNano(0);
            if (registrationOpenAt.isBefore(currentMinute)) {
                throw new AppException(ErrorCode.REGISTRATION_OPEN_TIME_IN_PAST);
            }
        }
        if (registrationCloseAt.isBefore(
                TournamentTimelinePolicy.minimumRegistrationCloseAt(
                        registrationOpenAt, phaseDays.get("REGISTRATION")))) {
            throw new AppException(ErrorCode.REGISTRATION_PERIOD_TOO_SHORT);
        }
        if (reviewDeadlineAt.isBefore(
                TournamentTimelinePolicy.minimumReviewDeadlineAt(
                        registrationCloseAt, phaseDays.get("REVIEW")))) {
            throw new AppException(ErrorCode.REVIEW_PERIOD_TOO_SHORT);
        }
        if (jockeyMatchingDeadlineAt.isBefore(
                TournamentTimelinePolicy.minimumJockeyMatchingDeadlineAt(
                        reviewDeadlineAt, phaseDays.get("JOCKEY_MATCHING")))) {
            throw new AppException(ErrorCode.JOCKEY_MATCHING_PERIOD_TOO_SHORT);
        }
        if (schedulingDeadlineAt.isBefore(
                TournamentTimelinePolicy.minimumSchedulingDeadlineAt(
                        jockeyMatchingDeadlineAt, phaseDays.get("SCHEDULING")))) {
            throw new AppException(ErrorCode.SCHEDULING_PERIOD_TOO_SHORT);
        }
    }

    private TournamentResponse toResponse(Tournament tournament) {
        TournamentResponse response = tournamentMapper.toTournamentResponse(tournament);
        response.setRatingConfig(toRatingConfigResponse(tournament));
        response.setOverdue(calculateOverdue(tournament));
        List<TournamentPhaseConfig> phaseConfigs = tournamentPhaseConfigRepository
                .findByTournamentTournamentId(tournament.getTournamentId());
        Map<String, Integer> configs = new HashMap<>();
        for (TournamentPhaseConfig phaseConfig : phaseConfigs) {
            configs.put(phaseConfig.getPhaseName(), phaseConfig.getDurationDays());
        }
        if (!configs.isEmpty()) {
            response.setPhaseConfigs(configs);
        }
        return response;
    }

  private boolean calculateOverdue(Tournament tournament) {
        if (tournament == null || tournament.getPhase() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return switch (tournament.getPhase()) {
            case REGISTRATION_REVIEW -> isPastDeadline(now, tournament.getReviewDeadlineAt());
            case JOCKEY_MATCHING -> isPastDeadline(now, tournament.getJockeyMatchingDeadlineAt());
            case SCHEDULING -> isPastDeadline(now, tournament.getSchedulingDeadlineAt());
            default -> false;
        };
    }

    private boolean isPastDeadline(LocalDateTime now, LocalDateTime deadline) {
        return deadline != null && now.isAfter(deadline);
    }

    private boolean allRacesFinished(Tournament tournament) {
        List<Round> rounds = roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(
                tournament.getTournamentId());
        for (Round round : rounds) {
            List<Race> races = raceRepository.findByRound_RoundId(round.getRoundId());
            for (Race race : races) {
                if (race.getFinishedAt() == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void validateHandicapSettings(Boolean handicapEnabled, Integer topWeightLbs, Integer minWeightLbs, Double equipmentWeightKg) {
        if (handicapEnabled != null && handicapEnabled) {
            if (topWeightLbs == null || minWeightLbs == null || equipmentWeightKg == null) {
                throw new AppException(ErrorCode.HANDICAP_RULE_CANNOT_NULL);
            }
            if (topWeightLbs <= 0 || minWeightLbs <= 0 || equipmentWeightKg <= 0.0) {
                throw new AppException(ErrorCode.WEIGHT_MUST_POSTIVE);
            }
            if (minWeightLbs >= topWeightLbs) {
                throw new AppException(ErrorCode.INVALID_WEIGHT);
            }
        } else {
            if ((topWeightLbs != null && topWeightLbs != 0) ||
                    (minWeightLbs != null && minWeightLbs != 0) ||
                    (equipmentWeightKg != null && equipmentWeightKg != 0.0)) {
                throw new AppException(ErrorCode.HANDICAP_DISABLE);
            }
        }
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public List<TournamentResponse> getAll() {
        List<TournamentResponse> responses = new ArrayList<>();
        for (Tournament tournament : tournamentRepository.findAllByOrderByCreatedAtDesc()) {
            responses.add(toResponse(tournament));
        }
        return responses;
    }

    @Override
    public TournamentResponse getById(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        return toResponse(tournament);
    }

    private static final int DEFAULT_INSPECTION_OPEN_MINUTES = 60;
    private static final int DEFAULT_INSPECTION_CLOSE_MINUTES = 5;
    private static final int DEFAULT_PREDICTION_CLOSE_MINUTES = 5;
    private static final int DEFAULT_MIN_INTERVAL_MINUTES = 30;
    private static final int DEFAULT_START_EARLY_TOLERANCE = 0;
    private static final int DEFAULT_START_LATE_TOLERANCE = 30;
    private static final int DEFAULT_OPERATIONAL_MINUTES = 5;
    private static final int DEFAULT_OPEN_HOURS = 24;
    private static final LocalTime DEFAULT_DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_DAY_END = LocalTime.of(18, 0);

    private static final int MIN_INTERVAL_MIN = 1;
    private static final int MAX_INTERVAL_MIN = 30;
    private static final int MIN_INSPECTION_OPEN = 30;
    private static final int MAX_INSPECTION_OPEN = 90;

    @Override
    public TournamentRatingConfigResponse getDefaultRatingConfig() {
        return TournamentRatingConfigResponse.builder()
                .firstMin(horseRatingProperties.getFirstMin())
                .firstMax(horseRatingProperties.getFirstMax())
                .secondMin(horseRatingProperties.getSecondMin())
                .secondMax(horseRatingProperties.getSecondMax())
                .thirdMin(horseRatingProperties.getThirdMin())
                .thirdMax(horseRatingProperties.getThirdMax())
                .fourthFifthMin(horseRatingProperties.getFourthFifthMin())
                .fourthFifthMax(horseRatingProperties.getFourthFifthMax())
                .otherMin(horseRatingProperties.getOtherMin())
                .otherMax(horseRatingProperties.getOtherMax())
                .disqualifiedMin(horseRatingProperties.getDisqualifiedMin())
                .disqualifiedMax(horseRatingProperties.getDisqualifiedMax())
                .policyVersion(horseRatingProperties.getPolicyVersion())
                .locked(false)
                .lockedAt(null)
                .build();
    }

    private void applyInitialRatingConfig(
            Tournament tournament, TournamentRatingConfigRequest request) {
        int firstMin = valueOrDefault(
                request == null ? null : request.getFirstMin(), horseRatingProperties.getFirstMin());
        int firstMax = valueOrDefault(
                request == null ? null : request.getFirstMax(), horseRatingProperties.getFirstMax());
        int secondMin = valueOrDefault(
                request == null ? null : request.getSecondMin(), horseRatingProperties.getSecondMin());
        int secondMax = valueOrDefault(
                request == null ? null : request.getSecondMax(), horseRatingProperties.getSecondMax());
        int thirdMin = valueOrDefault(
                request == null ? null : request.getThirdMin(), horseRatingProperties.getThirdMin());
        int thirdMax = valueOrDefault(
                request == null ? null : request.getThirdMax(), horseRatingProperties.getThirdMax());
        int fourthFifthMin = valueOrDefault(
                request == null ? null : request.getFourthFifthMin(),
                horseRatingProperties.getFourthFifthMin());
        int fourthFifthMax = valueOrDefault(
                request == null ? null : request.getFourthFifthMax(),
                horseRatingProperties.getFourthFifthMax());
        int otherMin = valueOrDefault(
                request == null ? null : request.getOtherMin(), horseRatingProperties.getOtherMin());
        int otherMax = valueOrDefault(
                request == null ? null : request.getOtherMax(), horseRatingProperties.getOtherMax());
        int disqualifiedMin = valueOrDefault(
                request == null ? null : request.getDisqualifiedMin(),
                horseRatingProperties.getDisqualifiedMin());
        int disqualifiedMax = valueOrDefault(
                request == null ? null : request.getDisqualifiedMax(),
                horseRatingProperties.getDisqualifiedMax());

        validateRatingConfig(
                firstMin, firstMax, secondMin, secondMax, thirdMin, thirdMax,
                fourthFifthMin, fourthFifthMax, otherMin, otherMax,
                disqualifiedMin, disqualifiedMax);
        setRatingConfig(
                tournament, firstMin, firstMax, secondMin, secondMax, thirdMin, thirdMax,
                fourthFifthMin, fourthFifthMax, otherMin, otherMax,
                disqualifiedMin, disqualifiedMax);
        tournament.setRatingPolicyVersion(horseRatingProperties.getPolicyVersion());
        tournament.setRatingPolicyLockedAt(null);
    }

    private void applyRatingConfigUpdate(
            Tournament tournament, TournamentRatingConfigRequest request) {
        if (request == null) {
            return;
        }

        int firstMin = valueOrDefault(request.getFirstMin(), tournament.getRatingFirstMin());
        int firstMax = valueOrDefault(request.getFirstMax(), tournament.getRatingFirstMax());
        int secondMin = valueOrDefault(request.getSecondMin(), tournament.getRatingSecondMin());
        int secondMax = valueOrDefault(request.getSecondMax(), tournament.getRatingSecondMax());
        int thirdMin = valueOrDefault(request.getThirdMin(), tournament.getRatingThirdMin());
        int thirdMax = valueOrDefault(request.getThirdMax(), tournament.getRatingThirdMax());
        int fourthFifthMin = valueOrDefault(
                request.getFourthFifthMin(), tournament.getRatingFourthFifthMin());
        int fourthFifthMax = valueOrDefault(
                request.getFourthFifthMax(), tournament.getRatingFourthFifthMax());
        int otherMin = valueOrDefault(request.getOtherMin(), tournament.getRatingOtherMin());
        int otherMax = valueOrDefault(request.getOtherMax(), tournament.getRatingOtherMax());
        int disqualifiedMin = valueOrDefault(
                request.getDisqualifiedMin(), tournament.getRatingDisqualifiedMin());
        int disqualifiedMax = valueOrDefault(
                request.getDisqualifiedMax(), tournament.getRatingDisqualifiedMax());

        validateRatingConfig(
                firstMin, firstMax, secondMin, secondMax, thirdMin, thirdMax,
                fourthFifthMin, fourthFifthMax, otherMin, otherMax,
                disqualifiedMin, disqualifiedMax);

        boolean changed = firstMin != tournament.getRatingFirstMin()
                || firstMax != tournament.getRatingFirstMax()
                || secondMin != tournament.getRatingSecondMin()
                || secondMax != tournament.getRatingSecondMax()
                || thirdMin != tournament.getRatingThirdMin()
                || thirdMax != tournament.getRatingThirdMax()
                || fourthFifthMin != tournament.getRatingFourthFifthMin()
                || fourthFifthMax != tournament.getRatingFourthFifthMax()
                || otherMin != tournament.getRatingOtherMin()
                || otherMax != tournament.getRatingOtherMax()
                || disqualifiedMin != tournament.getRatingDisqualifiedMin()
                || disqualifiedMax != tournament.getRatingDisqualifiedMax();

        setRatingConfig(
                tournament, firstMin, firstMax, secondMin, secondMax, thirdMin, thirdMax,
                fourthFifthMin, fourthFifthMax, otherMin, otherMax,
                disqualifiedMin, disqualifiedMax);
        if (changed) {
            int currentVersion = tournament.getRatingPolicyVersion();
            tournament.setRatingPolicyVersion(Math.max(1, currentVersion) + 1);
        }
    }

    private void validateRatingConfig(
            int firstMin, int firstMax,
            int secondMin, int secondMax,
            int thirdMin, int thirdMax,
            int fourthFifthMin, int fourthFifthMax,
            int otherMin, int otherMax,
            int disqualifiedMin, int disqualifiedMax) {
        boolean valid = firstMin >= 0
                && firstMin <= firstMax
                && secondMin >= 0
                && secondMin <= secondMax
                && thirdMin >= 0
                && thirdMin <= thirdMax
                && fourthFifthMin >= 0
                && fourthFifthMin <= fourthFifthMax
                && otherMin <= otherMax
                && otherMax <= 0
                && disqualifiedMin <= disqualifiedMax
                && disqualifiedMax <= 0;
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_HORSE_RATING_CONFIG);
        }
    }

    private void setRatingConfig(
            Tournament tournament,
            int firstMin, int firstMax,
            int secondMin, int secondMax,
            int thirdMin, int thirdMax,
            int fourthFifthMin, int fourthFifthMax,
            int otherMin, int otherMax,
            int disqualifiedMin, int disqualifiedMax) {
        tournament.setRatingFirstMin(firstMin);
        tournament.setRatingFirstMax(firstMax);
        tournament.setRatingSecondMin(secondMin);
        tournament.setRatingSecondMax(secondMax);
        tournament.setRatingThirdMin(thirdMin);
        tournament.setRatingThirdMax(thirdMax);
        tournament.setRatingFourthFifthMin(fourthFifthMin);
        tournament.setRatingFourthFifthMax(fourthFifthMax);
        tournament.setRatingOtherMin(otherMin);
        tournament.setRatingOtherMax(otherMax);
        tournament.setRatingDisqualifiedMin(disqualifiedMin);
        tournament.setRatingDisqualifiedMax(disqualifiedMax);
    }

    private TournamentRatingConfigResponse toRatingConfigResponse(Tournament tournament) {
        LocalDateTime lockedAt = tournament.getRatingPolicyLockedAt();
        return TournamentRatingConfigResponse.builder()
                .firstMin(tournament.getRatingFirstMin())
                .firstMax(tournament.getRatingFirstMax())
                .secondMin(tournament.getRatingSecondMin())
                .secondMax(tournament.getRatingSecondMax())
                .thirdMin(tournament.getRatingThirdMin())
                .thirdMax(tournament.getRatingThirdMax())
                .fourthFifthMin(tournament.getRatingFourthFifthMin())
                .fourthFifthMax(tournament.getRatingFourthFifthMax())
                .otherMin(tournament.getRatingOtherMin())
                .otherMax(tournament.getRatingOtherMax())
                .disqualifiedMin(tournament.getRatingDisqualifiedMin())
                .disqualifiedMax(tournament.getRatingDisqualifiedMax())
                .policyVersion(tournament.getRatingPolicyVersion())
                .locked(lockedAt != null)
                .lockedAt(lockedAt)
                .build();
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private void validateSchedulingAndTimeline(
            Integer insOpen, Integer insClose, Integer predClose,
            Integer minInterval, Integer startEarly, Integer startLate,
            Integer operational, Integer openHours,
            LocalTime dayStart, LocalTime dayEnd,
            Boolean applyBreak, LocalTime breakStart, LocalTime breakEnd) {

        int io = insOpen != null ? insOpen : DEFAULT_INSPECTION_OPEN_MINUTES;
        int ic = insClose != null ? insClose : DEFAULT_INSPECTION_CLOSE_MINUTES;
        int pc = predClose != null ? predClose : DEFAULT_PREDICTION_CLOSE_MINUTES;

        if (!(io > ic && ic >= pc && pc >= 0)) {
            throw new AppException(ErrorCode.INVALID_INSPECTION_TIMELINE);
        }

        int mi = minInterval != null ? minInterval : DEFAULT_MIN_INTERVAL_MINUTES;
        int se = startEarly != null ? startEarly : DEFAULT_START_EARLY_TOLERANCE;
        int sl = startLate != null ? startLate : DEFAULT_START_LATE_TOLERANCE;
        int op = operational != null ? operational : DEFAULT_OPERATIONAL_MINUTES;
        int oh = openHours != null ? openHours : DEFAULT_OPEN_HOURS;

        if (mi < MIN_INTERVAL_MIN || mi > MAX_INTERVAL_MIN
                || se < 0 || sl < 0 || op < 1 || oh < 1
                || io < MIN_INSPECTION_OPEN || io > MAX_INSPECTION_OPEN || ic < 1) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }

        LocalTime ds = dayStart != null ? dayStart : DEFAULT_DAY_START;
        LocalTime de = dayEnd != null ? dayEnd : DEFAULT_DAY_END;

        if (!ds.isBefore(de)) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }

        boolean applyB = applyBreak != null ? applyBreak : false;
        if (applyB) {
            if (breakStart == null || breakEnd == null) {
                throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
            }
            if (!(ds.isBefore(breakStart) && breakStart.isBefore(breakEnd) && breakEnd.isBefore(de))) {
                throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
            }
        } else {
            if (breakStart != null || breakEnd != null) {
                throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
            }
        }
    }

    private void validateMaxApprovedEntries(Integer maxEntries) {
        if (maxEntries == null || maxEntries < 1) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    @Override
    @Transactional
    public TournamentResponse uploadImage(UUID id, MultipartFile file) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        try {
            String imageUrl = cloudinaryService.uploadImage(file, "tournaments");
            tournament.setImageUrl(imageUrl);
            return toResponse(tournamentRepository.save(tournament));
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private Map<String, Integer> resolvePhaseConfigs(Map<String, Integer> requestConfigs,
                                                       int maxApprovedEntries) {
        if (requestConfigs != null && !requestConfigs.isEmpty()) {
            return requestConfigs;
        }
        return getDefaultPhaseConfigs(maxApprovedEntries);
    }

    public Map<String, Integer> getDefaultPhaseConfigs(int maxApprovedEntries) {
        Map<String, Integer> configs = new HashMap<>();
        configs.put("REGISTRATION", phaseTimingConfigRepository
                .findByPhaseNameAndCapacity("REGISTRATION", maxApprovedEntries)
                .map(PhaseTimingConfig::getDurationDays).orElse(0));
        configs.put("REVIEW", phaseTimingConfigRepository
                .findByPhaseNameAndCapacity("REVIEW", maxApprovedEntries)
                .map(PhaseTimingConfig::getDurationDays).orElse(0));
        configs.put("JOCKEY_MATCHING", phaseTimingConfigRepository
                .findByPhaseNameAndCapacity("JOCKEY_MATCHING", maxApprovedEntries)
                .map(PhaseTimingConfig::getDurationDays).orElse(0));
        configs.put("SCHEDULING", phaseTimingConfigRepository
                .findByPhaseNameAndCapacity("SCHEDULING", maxApprovedEntries)
                .map(PhaseTimingConfig::getDurationDays).orElse(0));
        configs.put("PRE_RACE_BUFFER", phaseTimingConfigRepository
                .findByPhaseNameAndCapacity("PRE_RACE_BUFFER", maxApprovedEntries)
                .map(PhaseTimingConfig::getDurationDays).orElse(0));
        return configs;
    }

    private void savePhaseConfigs(UUID tournamentId, Map<String, Integer> configs) {
        tournamentPhaseConfigRepository.deleteByTournamentTournamentId(tournamentId);
        Tournament tournamentRef = tournamentRepository.getReferenceById(tournamentId);
        List<TournamentPhaseConfig> entities = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : configs.entrySet()) {
            TournamentPhaseConfig entity = TournamentPhaseConfig.builder()
                        .tournament(tournamentRef)
                        .phaseName(entry.getKey())
                        .durationDays(entry.getValue())
                        .build();
            entities.add(entity);
        }
        tournamentPhaseConfigRepository.saveAll(entities);
    }
}
