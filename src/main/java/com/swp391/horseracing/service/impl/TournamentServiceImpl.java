package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.enums.RoundStatus;
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
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import com.swp391.horseracing.service.TournamentService;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    RaceEntryRepository raceEntryRepository;
    RaceRefereeRepository raceRefereeRepository;
    HorseTournamentRegistrationRepository horseRegistrationRepository;
    InvoiceRepository invoiceRepository;
    InvoiceService invoiceService;
    BusinessNotificationEventService notificationEventService;

    @Override
    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
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

        tournamentMapper.updateTournament(request, tournament);

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

        if (prizeStructureRepository.findByTournament_TournamentId(id).isEmpty()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_PRIZE);
        }

        if (eligibilityRepository.findByTournament_TournamentId(id).isEmpty()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_ELIGIBILITY);
        }

        List<Round> rounds = roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(id);
        if (rounds.size() != tournament.getMaxRounds()) {
            throw new AppException(ErrorCode.TOURNAMENT_MISSING_ROUNDS);
        }

        for (Round round : rounds) {
            if (raceRepository.findByRound_RoundId(round.getRoundId()).isEmpty()) {
                throw new AppException(ErrorCode.ROUND_MISSING_RACES);
            }
        }

        Round firstRound = rounds.get(0);

        if (tournament.getSchedulingDeadlineAt().toLocalDate().isAfter(firstRound.getStartDate().toLocalDate())) {
            throw new AppException(ErrorCode.SCHEDULING_DEADLINE_AFTER_ROUND);
        }

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
        setPhaseAndStatus(tournament, TournamentPhase.SCHEDULING);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Override
    @Transactional
    public TournamentResponse publishSchedule(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        validatePhase(tournament, TournamentPhase.SCHEDULING);

        List<Race> races = raceRepository.findByRound_Tournament_TournamentId(id);
        if (races.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG);
        }

        for (Race race : races) {
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

        LocalDateTime firstRaceStartTime = races.stream()
                .map(Race::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_SCHEDULING_CONFIG));

        LocalDateTime commonPredictionOpenAt = firstRaceStartTime.minusHours(tournament.getPredictionCardOpenHoursBeforeFirstRace());

        for (Race race : races) {
            race.setStatus(RoundStatus.SCHEDULED);
            race.setSchedulePublishedAt(LocalDateTime.now());
            race.setPredictionOpenAt(commonPredictionOpenAt);
            race.setPredictionCloseAt(race.getStartTime().minusMinutes(tournament.getPredictionCloseMinutesBefore()));
            raceRepository.save(race);
        }

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
        return tournamentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
}
