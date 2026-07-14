package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.request.ConfirmBracketRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.dto.tournament.response.BracketPreviewResponse;
import com.swp391.horseracing.dto.tournament.response.RoundPreviewDto;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.BracketPlanStatus;
import com.swp391.horseracing.enums.RoundTransitionStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TournamentMapper;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentEligibilityRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import com.swp391.horseracing.service.TournamentService;
import com.swp391.horseracing.service.RaceService;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private static final int MIN_ENTRIES_PER_RACE = 8;
    private static final int MAX_ENTRIES_PER_RACE = 16;
    private static final int QUALIFIERS_PER_RACE = 4;
    private static final int JOCKEY_POOL_PERCENT = 125;

    TournamentRepository tournamentRepository;
    UserRepository userRepository;
    TournamentMapper tournamentMapper;
    PrizeStructureRepository prizeStructureRepository;
    TournamentEligibilityRepository eligibilityRepository;
    RoundRepository roundRepository;
    RaceRepository raceRepository;
    RaceEntryRepository raceEntryRepository;
    RaceRefereeRepository raceRefereeRepository;
    HorseTournamentRegistrationRepository horseRegistrationRepository;
    InvoiceRepository invoiceRepository;
    InvoiceService invoiceService;
    BusinessNotificationEventService notificationEventService;
    JockeyHorseContractRepository jockeyHorseContractRepository;
    RaceService raceService;

    @Override
    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
        }

        Integer maxEntries = request.getMaxApprovedEntries();
        validateMaxApprovedEntries(maxEntries);
        validateJockeyCapacity(maxEntries, request.getMaxApprovedJockeys());
        if (request.getMinRoundGapDays() == null || request.getMinRoundGapDays() < 7) {
            throw new AppException(ErrorCode.ROUND_GAP_TOO_SHORT);
        }

        if (tournamentRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TOURNAMENT_NAME_EXISTS);
        }

        validateSchedulingAndTimeline(
                request.getInspectionOpenMinutesBefore(),
                request.getInspectionCloseMinutesBefore(),
                request.getPredictionCloseMinutesBefore(),
                request.getMaxRacesPerDay(),
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

        validateHandicapSettings(request.getHandicapEnabled(), request.getTopWeightLbs(), request.getMinWeightLbs(), request.getEquipmentWeightKg());

        User currentUser = getCurrentUser();

        Tournament tournament = tournamentMapper.toTournament(request);
        
        if (!tournament.isHandicapEnabled()) {
            tournament.setTopWeightLbs(0);
            tournament.setMinWeightLbs(0);
            tournament.setEquipmentWeightKg(0.0);
        }

        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPhase(TournamentPhase.DRAFT);
        tournament.setMaxApprovedEntries(maxEntries);
        tournament.setMaxApprovedHorses(maxEntries);
        tournament.setBracketPlanStatus(BracketPlanStatus.NOT_GENERATED);
        tournament.setBracketPlanVersion(1);
        tournament.setCreatedBy(currentUser);
        tournament.setCreatedAt(LocalDateTime.now());

        return toResponse(tournamentRepository.save(tournament));
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

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
        }
        if (request.getStartDate() != null && request.getEndDate() == null
                && tournament.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
        }
        if (request.getStartDate() == null && request.getEndDate() != null
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
        Integer maxRaces = request.getMaxRacesPerDay() != null ? request.getMaxRacesPerDay() : tournament.getMaxRacesPerDay();
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

        validateSchedulingAndTimeline(insOpen, insClose, predClose, maxRaces, minInterval, startEarly, startLate, operational, openHours, dayStart, dayEnd, applyBreak, breakStart, breakEnd);

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
            Integer maxEntries = request.getMaxApprovedEntries();
            validateMaxApprovedEntries(maxEntries);
            if (tournament.getBracketPlanStatus() != BracketPlanStatus.NOT_GENERATED
                    && !maxEntries.equals(tournament.getMaxApprovedEntries())) {
                throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
            }
        }

        Integer effectiveMaxEntries = request.getMaxApprovedEntries() != null
                ? request.getMaxApprovedEntries() : tournament.getMaxApprovedEntries();
        Integer effectiveMaxJockeys = request.getMaxApprovedJockeys() != null
                ? request.getMaxApprovedJockeys() : tournament.getMaxApprovedJockeys();
        validateJockeyCapacity(effectiveMaxEntries, effectiveMaxJockeys);
        if (request.getMinRoundGapDays() != null && request.getMinRoundGapDays() < 7) {
            throw new AppException(ErrorCode.ROUND_GAP_TOO_SHORT);
        }

        tournamentMapper.updateTournament(request, tournament);
        tournament.setMaxApprovedHorses(tournament.getMaxApprovedEntries());

        if (!tournament.isHandicapEnabled()) {
            tournament.setTopWeightLbs(0);
            tournament.setMinWeightLbs(0);
            tournament.setEquipmentWeightKg(0.0);
        }

        return toResponse(tournamentRepository.save(tournament));
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

        if (tournament.getBracketPlanStatus() != com.swp391.horseracing.enums.BracketPlanStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BRACKET_NOT_CONFIRMED);
        }

        if (prizeStructureRepository.findByTournament_TournamentId(id).isEmpty()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_PRIZE);
        }

        if (eligibilityRepository.findByTournament_TournamentId(id).isEmpty()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_ELIGIBILITY);
        }

        validateBracketStructure(tournament);

        tournament.setPublishedAt(LocalDateTime.now());

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

        if (tournament.getBracketPlanStatus() != BracketPlanStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BRACKET_NOT_CONFIRMED);
        }
        validateBracketStructure(tournament);

        long actualApprovedCount = jockeyHorseContractRepository.countByTournament_TournamentIdAndStatus(id, com.swp391.horseracing.enums.ContractStatus.APPROVED);
        int actualCount = (int) actualApprovedCount;

        Integer maxEntries = tournament.getMaxApprovedEntries();
        validateMaxApprovedEntries(maxEntries);

        int firstRoundRaceCount = calculateFirstRoundRaceCount(maxEntries);

        int minEntriesRequired = firstRoundRaceCount * MIN_ENTRIES_PER_RACE;

        if (actualCount < minEntriesRequired || actualCount > maxEntries) {
            tournament.setBracketPlanStatus(BracketPlanStatus.STALE);
            return toResponse(tournamentRepository.save(tournament));
        } else {
            List<Round> rounds = roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(id);
            if (!rounds.isEmpty()) {
                Round round1 = rounds.get(0);
                updateExpectedEntriesAfterMatching(rounds, actualCount);
                int round1Entries = raceEntryRepository.countByRace_Round_RoundId(round1.getRoundId());
                if (round1Entries == 0) {
                    distributeFirstRoundEntries(tournament, round1);
                } else if (round1Entries != actualCount) {
                    throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
                }
            }
        }

        setPhaseAndStatus(tournament, TournamentPhase.SCHEDULING);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Override
    @Transactional
    public TournamentResponse publishSchedule(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getBracketPlanStatus() == com.swp391.horseracing.enums.BracketPlanStatus.STALE) {
            throw new AppException(ErrorCode.BRACKET_PLAN_STALE);
        }
        if (tournament.getBracketPlanStatus() != com.swp391.horseracing.enums.BracketPlanStatus.CONFIRMED &&
                tournament.getBracketPlanStatus() != com.swp391.horseracing.enums.BracketPlanStatus.LOCKED) {
            throw new AppException(ErrorCode.BRACKET_NOT_CONFIRMED);
        }

        validatePhase(tournament, TournamentPhase.SCHEDULING);
        validateBracketStructure(tournament);

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

        List<Race> racesToPublish = raceRepository.findByRound_RoundId(activeRound.getRoundId());
        if (racesToPublish.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }
        raceService.validateRoundScheduleForPublication(activeRound.getRoundId());

        for (Race race : racesToPublish) {
            if (race.getStatus() != RoundStatus.SCHEDULING && race.getStatus() != RoundStatus.SCHEDULED) {
                throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
            }
            int entryCount = raceEntryRepository.countByRace_RaceId(race.getRaceId());
            if (entryCount < race.getRound().getMinEntries()) {
                throw new AppException(ErrorCode.RACE_NOT_ENOUGH_ENTRIES);
            }
            int refereeCount = raceRefereeRepository.countByRace_RaceId(race.getRaceId());
            if (refereeCount < 1) {
                throw new AppException(ErrorCode.RACE_MISSING_REFEREES);
            }
        }

        // Validate gap between active round and previous round
        int gapDays = tournament.getMinRoundGapDays();
        if (activeRound.getSequenceOrder() > 1) {
            Round prevRound = orderedRounds.get(activeRound.getSequenceOrder() - 2);

            LocalDateTime lastRaceEnd = null;
            for (Race race : raceRepository.findByRound_RoundId(prevRound.getRoundId())) {
                if (race.getStatus() != RoundStatus.CANCELLED && race.getEndTime() != null
                        && (lastRaceEnd == null || race.getEndTime().isAfter(lastRaceEnd))) {
                    lastRaceEnd = race.getEndTime();
                }
            }

            LocalDateTime firstRaceStart = null;
            for (Race race : racesToPublish) {
                if (race.getStatus() != RoundStatus.CANCELLED && race.getStartTime() != null
                        && (firstRaceStart == null || race.getStartTime().isBefore(firstRaceStart))) {
                    firstRaceStart = race.getStartTime();
                }
            }

            if (lastRaceEnd != null && firstRaceStart != null
                    && firstRaceStart.isBefore(lastRaceEnd.plusDays(gapDays))) {
                throw new AppException(ErrorCode.ROUND_GAP_TOO_SHORT);
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

        tournament.setCurrentRoundName(activeRound.getRoundName());
        setPhaseAndStatus(tournament, TournamentPhase.RACING);
        
        if (activeRound.getSequenceOrder() == 1) {
            tournament.setBracketPlanStatus(com.swp391.horseracing.enums.BracketPlanStatus.LOCKED);
        }

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
        prizes.forEach(prize -> prize.setActive(true));
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

    private TournamentResponse toResponse(Tournament tournament) {
        TournamentResponse response = tournamentMapper.toTournamentResponse(tournament);
        response.setOverdue(calculateOverdue(tournament));
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
            if (races.stream().anyMatch(r -> r.getFinishedAt() == null)) {
                return false;
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
    private void validateSchedulingAndTimeline(
            Integer insOpen, Integer insClose, Integer predClose,
            Integer maxRaces, Integer minInterval, Integer startEarly, Integer startLate,
            Integer operational, Integer openHours,
            LocalTime dayStart, LocalTime dayEnd,
            Boolean applyBreak, LocalTime breakStart, LocalTime breakEnd) {

        int io = insOpen != null ? insOpen : 90;
        int ic = insClose != null ? insClose : 30;
        int pc = predClose != null ? predClose : 5;

        if (!(io > ic && ic > pc && pc >= 0)) {
            throw new AppException(ErrorCode.INVALID_INSPECTION_TIMELINE);
        }

        int mr = maxRaces != null ? maxRaces : 9;
        int mi = minInterval != null ? minInterval : 35;
        int se = startEarly != null ? startEarly : 0;
        int sl = startLate != null ? startLate : 30;
        int op = operational != null ? operational : 30;
        int oh = openHours != null ? openHours : 24;

        if (mr < 1 || mr > 9 || mi < 30 || mi > 60 || se < 0 || sl < 0 || op < 1 || oh < 1) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }

        LocalTime ds = dayStart != null ? dayStart : LocalTime.of(8, 0);
        LocalTime de = dayEnd != null ? dayEnd : LocalTime.of(18, 0);

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

    @Override
    public com.swp391.horseracing.dto.tournament.response.BracketPreviewResponse getBracketPreview(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        Integer maxEntries = tournament.getMaxApprovedEntries();
        validateMaxApprovedEntries(maxEntries);

        long actualApprovedCount = jockeyHorseContractRepository.countByTournament_TournamentIdAndStatus(tournamentId, com.swp391.horseracing.enums.ContractStatus.APPROVED);
        int actualCount = (int) actualApprovedCount;

        int effectiveCount = actualCount > 0 ? actualCount : maxEntries;

        int firstRoundRaceCount = calculateFirstRoundRaceCount(maxEntries);

        int minEntriesRequired = firstRoundRaceCount * 8;

        boolean valid = true;
        String errorMessage = null;
        Integer recommendedMax = null;

        boolean actualCountIsFinal = actualCount > 0
                || tournament.getBracketPlanStatus() == BracketPlanStatus.STALE;
        if (actualCountIsFinal) {
            if (actualCount < minEntriesRequired) {
                valid = false;
                errorMessage = "Số lượng hồ sơ đã duyệt không đủ để duy trì cấu trúc bracket hiện tại (cần tối thiểu " + minEntriesRequired + " entry). Đề xuất giảm maxApprovedEntries.";
                recommendedMax = getRecommendedMaxApprovedEntries(actualCount);
            } else if (actualCount > maxEntries) {
                valid = false;
                errorMessage = "Số lượng hồ sơ đã duyệt vượt quá sức chứa tối đa của Tournament.";
            }
        }

        java.util.List<com.swp391.horseracing.dto.tournament.response.RoundPreviewDto> rounds = new java.util.ArrayList<>();
        int currentRaceCount = firstRoundRaceCount;
        int currentExpectedEntries = effectiveCount;
        int order = 1;

        while (true) {
            boolean isFinal = (currentRaceCount == 1);
            
            java.util.List<Integer> entriesPerRace = new java.util.ArrayList<>();
            if (order == 1) {
                int baseSize = currentExpectedEntries / currentRaceCount;
                int remainder = currentExpectedEntries % currentRaceCount;
                for (int i = 0; i < currentRaceCount; i++) {
                    entriesPerRace.add(i < remainder ? baseSize + 1 : baseSize);
                }
            } else {
                for (int i = 0; i < currentRaceCount; i++) {
                    entriesPerRace.add(8);
                }
            }

            rounds.add(com.swp391.horseracing.dto.tournament.response.RoundPreviewDto.builder()
                    .sequenceOrder(order)
                    .raceCount(currentRaceCount)
                    .entriesPerRace(entriesPerRace)
                    .isFinal(isFinal)
                    .build());

            if (isFinal) {
                break;
            }

            currentRaceCount /= 2;
            currentExpectedEntries = currentRaceCount * 8;
            order++;
        }

        int totalRaceCount = 0;
        for (com.swp391.horseracing.dto.tournament.response.RoundPreviewDto round : rounds) {
            totalRaceCount += round.getRaceCount();
        }

        int racesPerDay = calculateMaximumRacesPerDay(tournament);
        int minimumRacingDays = 0;
        for (com.swp391.horseracing.dto.tournament.response.RoundPreviewDto round : rounds) {
            minimumRacingDays += divideRoundUp(round.getRaceCount(), racesPerDay);
        }
        int idleDaysBetweenRounds = Math.max(0, tournament.getMinRoundGapDays() - 1);
        int minimumCalendarDays = minimumRacingDays
                + Math.max(0, rounds.size() - 1) * idleDaysBetweenRounds;
        LocalDate earliestEndDate = tournament.getStartDate()
                .plusDays(Math.max(0, minimumCalendarDays - 1));
        boolean scheduleFeasible = !earliestEndDate.isAfter(tournament.getEndDate());
        if (!scheduleFeasible) {
            valid = false;
            if (errorMessage == null) {
                errorMessage = "Tournament needs at least " + minimumCalendarDays
                        + " calendar days for the selected bracket";
            }
        }

        return com.swp391.horseracing.dto.tournament.response.BracketPreviewResponse.builder()
                .maxApprovedEntries(maxEntries)
                .actualApprovedEntries(actualCount)
                .minEntriesPerRace(8)
                .maxEntriesPerRace(16)
                .qualifiersPerRace(4)
                .predictionPositions(3)
                .finalPrizePositions(3)
                .rounds(rounds)
                .totalRaceCount(totalRaceCount)
                .valid(valid)
                .errorMessage(errorMessage)
                .recommendedMaxApprovedEntries(recommendedMax)
                .minimumRacingDays(minimumRacingDays)
                .minimumTournamentCalendarDays(minimumCalendarDays)
                .earliestPossibleEndDate(earliestEndDate)
                .scheduleFeasible(scheduleFeasible)
                .build();
    }

    private int getRecommendedMaxApprovedEntries(int actualApprovedEntries) {
        if (actualApprovedEntries < 8) {
            return 8;
        }
        int power = 8;
        while (power < actualApprovedEntries) {
            power *= 2;
        }
        return power;
    }

    @Override
    @Transactional
    public TournamentResponse confirmBracket(UUID tournamentId, ConfirmBracketRequest request) {
        Tournament tournament = tournamentRepository.findForUpdateByTournamentId(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        BracketPlanStatus previousStatus = tournament.getBracketPlanStatus();
        if (previousStatus == BracketPlanStatus.CONFIRMED
                || previousStatus == BracketPlanStatus.LOCKED) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
        }
        if (previousStatus == BracketPlanStatus.NOT_GENERATED
                && tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
        if (!request.getExpectedPlanVersion().equals(tournament.getBracketPlanVersion())) {
            throw new AppException(ErrorCode.BRACKET_PLAN_VERSION_CONFLICT);
        }

        Integer selectedMax = request.getMaxApprovedEntries();
        validateMaxApprovedEntries(selectedMax);
        validateJockeyCapacity(selectedMax, tournament.getMaxApprovedJockeys());
        if (previousStatus == BracketPlanStatus.NOT_GENERATED
                && !selectedMax.equals(tournament.getMaxApprovedEntries())) {
            throw new AppException(ErrorCode.INVALID_MAX_APPROVED_ENTRIES);
        }
        if (previousStatus == BracketPlanStatus.STALE) {
            BracketPreviewResponse stalePreview = getBracketPreview(tournamentId);
            if (stalePreview.getRecommendedMaxApprovedEntries() == null
                    || !selectedMax.equals(stalePreview.getRecommendedMaxApprovedEntries())) {
                throw new AppException(ErrorCode.INVALID_MAX_APPROVED_ENTRIES);
            }
        }
        tournament.setMaxApprovedEntries(selectedMax);
        tournament.setMaxApprovedHorses(selectedMax);

        com.swp391.horseracing.dto.tournament.response.BracketPreviewResponse preview = getBracketPreview(tournamentId);
        if (!preview.isValid()) {
            if (!preview.isScheduleFeasible()) {
                throw new AppException(ErrorCode.TOURNAMENT_DATE_RANGE_TOO_SHORT_FOR_BRACKET);
            }
            if (preview.getActualApprovedEntries() > selectedMax) {
                throw new AppException(ErrorCode.APPROVED_ENTRIES_EXCEED_MAXIMUM);
            }
            throw new AppException(ErrorCode.APPROVED_ENTRIES_BELOW_BRACKET_MINIMUM);
        }

        // Delete existing rounds and races
        List<Round> oldRounds = roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId);
        for (Round r : oldRounds) {
            raceEntryRepository.deleteAll(raceEntryRepository.findByRace_Round_RoundId(r.getRoundId()));
            raceRepository.deleteAll(raceRepository.findByRound_RoundId(r.getRoundId()));
        }
        roundRepository.deleteAll(oldRounds);
        roundRepository.flush();

        User currentUser = getCurrentUser();

        // Create new skeleton rounds and races
        int newPlanVersion = tournament.getBracketPlanVersion() + 1;
        java.util.List<Round> savedRounds = new java.util.ArrayList<>();
        for (com.swp391.horseracing.dto.tournament.response.RoundPreviewDto roundDto : preview.getRounds()) {
            Round round = Round.builder()
                    .roundName("Vòng " + roundDto.getSequenceOrder() + (roundDto.isFinal() ? " (Chung Kết)" : ""))
                    .sequenceOrder(roundDto.getSequenceOrder())
                    .isFinal(roundDto.isFinal())
                    .predictionType(com.swp391.horseracing.enums.PredictionType.TOP3)
                    .advancementRule(roundDto.isFinal() ? "Xác định hạng chung cuộc nhận giải thưởng" : "Top 4 mỗi Race đi tiếp vào vòng sau")
                    .startDate(null)
                    .endDate(null)
                    .description("Vòng đấu số " + roundDto.getSequenceOrder() + " thuộc giải " + tournament.getName())
                    .maxRaces(roundDto.getRaceCount())
                    .plannedRaceCount(roundDto.getRaceCount())
                    .expectedEntries(sumEntries(roundDto.getEntriesPerRace()))
                    .maxEntries(16)
                    .minEntries(8)
                    .qualifiersPerRace(roundDto.isFinal() ? 0 : 4)
                    .bracketPlanVersion(newPlanVersion)
                    .transitionStatus(RoundTransitionStatus.NOT_READY)
                    .status(com.swp391.horseracing.enums.RoundStatus.SCHEDULING)
                    .tournament(tournament)
                    .createdBy(currentUser)
                    .createdAt(LocalDateTime.now())
                    .build();
            Round savedRound = roundRepository.save(round);
            savedRounds.add(savedRound);

            for (int j = 1; j <= roundDto.getRaceCount(); j++) {
                Race race = Race.builder()
                        .name(tournament.getName() + " - " + savedRound.getRoundName() + " - Race " + j)
                        .startTime(null)
                        .endTime(null)
                        .trackCondition("TBD")
                        .distance(1000.0f)
                        .sequenceOrder(j)
                        .status(com.swp391.horseracing.enums.RoundStatus.SCHEDULING)
                        .predictionOpenAt(null)
                        .predictionCloseAt(null)
                        .round(savedRound)
                        .createdBy(currentUser)
                        .build();
                raceRepository.save(race);
            }
        }

        // If actual approved entries exist, distribute them immediately
        if (preview.getActualApprovedEntries() > 0 && !savedRounds.isEmpty()) {
            distributeFirstRoundEntries(tournament, savedRounds.get(0));
            updateExpectedEntriesAfterMatching(savedRounds, preview.getActualApprovedEntries());
        }

        tournament.setPlannedRoundCount(preview.getRounds().size());
        tournament.setPlannedRaceCount(preview.getTotalRaceCount());
        tournament.setMaxRounds(preview.getRounds().size());
        tournament.setBracketPlanVersion(newPlanVersion);
        tournament.setBracketPlanStatus(BracketPlanStatus.CONFIRMED);
        if (previousStatus == BracketPlanStatus.STALE) {
            tournament.setPhase(TournamentPhase.SCHEDULING);
        }

        return toResponse(tournamentRepository.save(tournament));
    }

    private void distributeFirstRoundEntries(Tournament tournament, Round round1) {
        List<JockeyHorseContract> approvedContracts = jockeyHorseContractRepository.findByTournament_TournamentIdAndStatus(
                tournament.getTournamentId(), com.swp391.horseracing.enums.ContractStatus.APPROVED);
        List<Race> round1Races = raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(round1.getRoundId());
        if (round1Races.isEmpty() || approvedContracts.isEmpty()) {
            return;
        }
        if (raceEntryRepository.countByRace_Round_RoundId(round1.getRoundId()) > 0) {
            throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
        }

        approvedContracts.sort(new Comparator<JockeyHorseContract>() {
            @Override
            public int compare(JockeyHorseContract left, JockeyHorseContract right) {
                int ratingCompare = Integer.compare(
                        right.getHorse().getCurrentRating(), left.getHorse().getCurrentRating());
                if (ratingCompare != 0) {
                    return ratingCompare;
                }
                return left.getContractId().toString().compareTo(right.getContractId().toString());
            }
        });

        Set<UUID> horseIds = new HashSet<>();
        Set<UUID> jockeyIds = new HashSet<>();
        Set<UUID> contractIds = new HashSet<>();
        int[] laneNumbers = new int[round1Races.size()];
        for (int index = 0; index < approvedContracts.size(); index++) {
            JockeyHorseContract contract = approvedContracts.get(index);
            if (!horseIds.add(contract.getHorse().getHorseId())
                    || !jockeyIds.add(contract.getJockey().getJockeyId())
                    || !contractIds.add(contract.getContractId())) {
                throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
            }

            int cycle = index / round1Races.size();
            int offset = index % round1Races.size();
            int raceIndex = cycle % 2 == 0 ? offset : round1Races.size() - 1 - offset;
            laneNumbers[raceIndex]++;
            RaceEntry entry = RaceEntry.builder()
                    .race(round1Races.get(raceIndex))
                    .contract(contract)
                    .laneNumber(laneNumbers[raceIndex])
                    .status(RaceEntryStatus.CONFIRMED)
                    .assignedBy(tournament.getCreatedBy())
                    .assignedAt(LocalDateTime.now())
                    .build();
            raceEntryRepository.save(entry);
        }

        for (int entryCount : laneNumbers) {
            if (entryCount < MIN_ENTRIES_PER_RACE || entryCount > MAX_ENTRIES_PER_RACE) {
                throw new AppException(ErrorCode.RACE_ENTRIES_OUT_OF_RANGE);
            }
        }
    }

    private int sumEntries(List<Integer> entriesPerRace) {
        int total = 0;
        for (Integer count : entriesPerRace) {
            total += count;
        }
        return total;
    }

    private int calculateMaximumRacesPerDay(Tournament tournament) {
        int operationalMinutes = tournament.getDefaultRaceOperationalMinutes();
        int intervalMinutes = tournament.getMinRaceIntervalMinutes();
        LocalTime candidateStart = tournament.getRaceDayStartTime();
        LocalTime dayEnd = tournament.getRaceDayEndTime();
        int capacity = 0;
        while (candidateStart != null && dayEnd != null
                && !candidateStart.plusMinutes(operationalMinutes).isAfter(dayEnd)) {
            LocalTime candidateEnd = candidateStart.plusMinutes(operationalMinutes);
            if (Boolean.TRUE.equals(tournament.getApplyBreakTime())
                    && overlaps(candidateStart, candidateEnd,
                    tournament.getBreakStartTime(), tournament.getBreakEndTime())) {
                candidateStart = tournament.getBreakEndTime();
                continue;
            }
            capacity++;
            candidateStart = candidateEnd.plusMinutes(intervalMinutes);
        }
        capacity = Math.min(capacity, tournament.getMaxRacesPerDay());
        if (capacity < 1) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }
        return capacity;
    }

    private boolean overlaps(LocalTime start, LocalTime end,
                             LocalTime breakStart, LocalTime breakEnd) {
        if (breakStart == null || breakEnd == null) {
            return false;
        }
        return start.isBefore(breakEnd) && end.isAfter(breakStart);
    }

    private int divideRoundUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private int calculateFirstRoundRaceCount(Integer maxEntries) {
        if (maxEntries <= MAX_ENTRIES_PER_RACE) {
            return 1;
        }
        return maxEntries / MAX_ENTRIES_PER_RACE;
    }

    private void validateMaxApprovedEntries(Integer maxEntries) {
        if (maxEntries == null || maxEntries < MIN_ENTRIES_PER_RACE
                || (maxEntries & (maxEntries - 1)) != 0) {
            throw new AppException(ErrorCode.INVALID_MAX_APPROVED_ENTRIES);
        }
    }

    private void validateJockeyCapacity(Integer maxEntries, Integer maxJockeys) {
        long minimumJockeys = ((long) maxEntries * JOCKEY_POOL_PERCENT + 99) / 100;
        if (maxJockeys == null || maxJockeys < minimumJockeys) {
            throw new AppException(ErrorCode.INVALID_JOCKEY_CAPACITY);
        }
    }

    private void updateExpectedEntriesAfterMatching(List<Round> rounds, int actualApprovedEntries) {
        for (int index = 0; index < rounds.size(); index++) {
            Round round = rounds.get(index);
            round.setExpectedEntries(index == 0
                    ? actualApprovedEntries
                    : round.getPlannedRaceCount() * MIN_ENTRIES_PER_RACE);
            roundRepository.save(round);
        }
    }

    private void validateBracketStructure(Tournament tournament) {
        if (tournament.getBracketPlanStatus() != BracketPlanStatus.CONFIRMED
                && tournament.getBracketPlanStatus() != BracketPlanStatus.LOCKED) {
            throw new AppException(ErrorCode.BRACKET_NOT_CONFIRMED);
        }
        List<Round> rounds = roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(
                tournament.getTournamentId());
        if (tournament.getPlannedRoundCount() == null
                || rounds.size() != tournament.getPlannedRoundCount()) {
            throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
        }

        int expectedRaceCount = calculateFirstRoundRaceCount(tournament.getMaxApprovedEntries());
        int totalRaces = 0;
        for (int index = 0; index < rounds.size(); index++) {
            Round round = rounds.get(index);
            boolean finalRound = index == rounds.size() - 1;
            if (round.getSequenceOrder() != index + 1
                    || round.isFinal() != finalRound
                    || !tournament.getBracketPlanVersion().equals(round.getBracketPlanVersion())
                    || round.getMinEntries() != MIN_ENTRIES_PER_RACE
                    || round.getMaxEntries() != MAX_ENTRIES_PER_RACE
                    || round.getPlannedRaceCount() == null
                    || round.getPlannedRaceCount() != expectedRaceCount
                    || (finalRound && round.getQualifiersPerRace() != 0)
                    || (!finalRound && round.getQualifiersPerRace() != QUALIFIERS_PER_RACE)) {
                throw new AppException(ErrorCode.ROUND_STRUCTURE_MISMATCH);
            }

            List<Race> races = raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(round.getRoundId());
            if (races.size() != expectedRaceCount || (finalRound && races.size() != 1)) {
                throw new AppException(ErrorCode.RACE_STRUCTURE_MISMATCH);
            }
            for (int raceIndex = 0; raceIndex < races.size(); raceIndex++) {
                if (races.get(raceIndex).getSequenceOrder() != raceIndex + 1) {
                    throw new AppException(ErrorCode.RACE_STRUCTURE_MISMATCH);
                }
            }
            totalRaces += races.size();
            expectedRaceCount /= 2;
        }
        if (tournament.getPlannedRaceCount() == null || totalRaces != tournament.getPlannedRaceCount()) {
            throw new AppException(ErrorCode.RACE_STRUCTURE_MISMATCH);
        }
    }
}
