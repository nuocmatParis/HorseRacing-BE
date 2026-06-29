package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TournamentMapper;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentEligibilityRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.TournamentService;
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

    @Override
    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_TOURNAMENT_DATES);
        }

        if (tournamentRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TOURNAMENT_NAME_EXISTS);
        }

        int openMin = request.getPredictionOpenMinutesBefore() != null
                ? request.getPredictionOpenMinutesBefore() : 120;
        int closeMin = request.getPredictionCloseMinutesBefore() != null
                ? request.getPredictionCloseMinutesBefore() : 5;
        if (openMin <= closeMin || closeMin < 0) {
            throw new AppException(ErrorCode.INVALID_PREDICTION_TIMES);
        }

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

        User currentUser = getCurrentUser();

        Tournament tournament = tournamentMapper.toTournament(request);
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPhase(TournamentPhase.DRAFT);
        tournament.setCreatedBy(currentUser);
        tournament.setCreatedAt(LocalDateTime.now());

        return toResponse(tournamentRepository.save(tournament));
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
        if (rounds.size() != 2) {
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
        return toResponse(tournamentRepository.save(tournament));
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
        setPhaseAndStatus(tournament, TournamentPhase.RACING);
        return toResponse(tournamentRepository.save(tournament));
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
}
