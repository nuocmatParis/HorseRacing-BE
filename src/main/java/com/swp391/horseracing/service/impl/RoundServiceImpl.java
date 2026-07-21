package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateRoundRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateRoundRequest;
import com.swp391.horseracing.dto.tournament.response.RoundResponse;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RoundMapper;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.RoundService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RoundServiceImpl implements RoundService {

    RoundRepository roundRepository;
    TournamentRepository tournamentRepository;
    UserRepository userRepository;
    RefereeRepository refereeRepository;
    RaceRefereeRepository raceRefereeRepository;
    com.swp391.horseracing.repository.RaceRepository raceRepository;
    RoundMapper roundMapper;

    @Override
    @Transactional
    public RoundResponse create(UUID tournamentId, CreateRoundRequest request) {
        validatePredictionType(request.getPredictionType());

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (roundRepository.existsByTournament_TournamentIdAndSequenceOrder(tournamentId, request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_ROUND_SEQUENCE);
        }

        if (roundRepository.existsByTournament_TournamentIdAndRoundName(tournamentId, request.getRoundName())) {
            throw new AppException(ErrorCode.ROUND_NAME_ALREADY_EXISTS);
        }

        if (request.getMinEntries() > request.getMaxEntries()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (Boolean.TRUE.equals(request.getIsFinal())) {
            if (roundRepository.existsByTournament_TournamentIdAndIsFinalTrue(tournamentId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            if (request.getMaxRaces() != null && request.getMaxRaces() != 1) {
                throw new AppException(ErrorCode.INVALID_FINAL_ROUND_CONFIGURATION);
            }
            if (request.getQualifiersPerRace() != null && request.getQualifiersPerRace() > 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            if (request.getQualifiersPerRace() == null || request.getQualifiersPerRace() < 1) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        if (request.getQualifiersPerRace() != null && request.getQualifiersPerRace() > request.getMaxEntries()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        List<Round> existingRounds = roundRepository
                .findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId);

        if (!existingRounds.isEmpty()) {
            Round lastRound = existingRounds.get(existingRounds.size() - 1);
            if (request.getStartDate().isBefore(lastRound.getEndDate())) {
                throw new AppException(ErrorCode.INVALID_ROUND_DATES);
            }
        }

        if (request.getStartDate().isBefore(tournament.getCompetitionStartAt())) {
            throw new AppException(ErrorCode.ROUND_DATES_OUT_OF_TOURNAMENT);
        }

        User currentUser = getCurrentUser();

        Round round = roundMapper.toRound(request);
        round.setTournament(tournament);
        round.setCreatedBy(currentUser);
        round.setCreatedAt(LocalDateTime.now());

        Round saved = roundRepository.save(round);

        if (request.getHeadRefereeId() != null) {
            Referee referee = refereeRepository.findById(request.getHeadRefereeId())
                    .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));
            if (referee.getStatus() == RefereeStatus.SUSPENDED) {
                throw new AppException(ErrorCode.REFEREE_NOT_AVAILABLE);
            }
            saved.setHeadReferee(referee);
            saved.setHeadRefereeAssignedAt(LocalDateTime.now());
            saved = roundRepository.save(saved);
        }

        return roundMapper.toRoundResponse(saved);
    }

    @Override
    @Transactional
    public RoundResponse update(UUID roundId, UpdateRoundRequest request) {
        if (request.getPredictionType() != null) {
            validatePredictionType(request.getPredictionType());
        }
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        Tournament tournament = round.getTournament();
        boolean scheduleEditable = tournament.getStatus() == TournamentStatus.DRAFT
                || tournament.getPhase() == TournamentPhase.SCHEDULING;
        if (!scheduleEditable) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
        if (tournament.getPhase() == TournamentPhase.SCHEDULING) {
            validateOnlyRoundScheduleChanges(request);
        }

        if (tournament.getStatus() != TournamentStatus.DRAFT && round.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.ROUND_ALREADY_SCHEDULED);
        }

        if (request.getMinEntries() != null && request.getMaxEntries() != null
                && request.getMinEntries() > request.getMaxEntries()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (Boolean.TRUE.equals(request.getIsFinal())
                && !round.isFinal()
                && roundRepository.existsByTournament_TournamentIdAndIsFinalTrue(tournament.getTournamentId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        boolean finalRound = request.getIsFinal() != null ? request.getIsFinal() : round.isFinal();
        Integer maxRaces = request.getMaxRaces() != null ? request.getMaxRaces() : round.getMaxRaces();
        if (finalRound && maxRaces != null && maxRaces != 1) {
            throw new AppException(ErrorCode.INVALID_FINAL_ROUND_CONFIGURATION);
        }

        Integer qualifiers = request.getQualifiersPerRace() != null
                ? request.getQualifiersPerRace() : round.getQualifiersPerRace();
        Integer maxEntries = request.getMaxEntries() != null ? request.getMaxEntries() : round.getMaxEntries();
        if (!finalRound && (qualifiers == null || qualifiers < 1)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (finalRound && qualifiers != null && qualifiers > 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (qualifiers != null && maxEntries != null && qualifiers > maxEntries) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getRoundName() != null && !request.getRoundName().equals(round.getRoundName())
                && roundRepository.existsByTournament_TournamentIdAndRoundName(tournament.getTournamentId(), request.getRoundName())) {
            throw new AppException(ErrorCode.ROUND_NAME_ALREADY_EXISTS);
        }

        if (request.getSequenceOrder() != null && request.getSequenceOrder() != round.getSequenceOrder()
                && roundRepository.existsByTournament_TournamentIdAndSequenceOrder(tournament.getTournamentId(), request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_ROUND_SEQUENCE);
        }

        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : round.getStartDate();

        if (request.getStartDate() != null && startDate.isBefore(tournament.getCompetitionStartAt())) {
            throw new AppException(ErrorCode.ROUND_DATES_OUT_OF_TOURNAMENT);
        }

        if (request.getStartDate() != null) {
            List<Round> allRounds = roundRepository
                    .findByTournament_TournamentIdOrderBySequenceOrderAsc(tournament.getTournamentId());
            for (Round r : allRounds) {
                if (r.getRoundId().equals(round.getRoundId())) continue;
                if (r.getEndDate() == null) continue;
                if (r.getSequenceOrder() < round.getSequenceOrder()
                        && startDate.isBefore(r.getEndDate())) {
                    throw new AppException(ErrorCode.INVALID_ROUND_DATES);
                }
            }
        }

        roundMapper.updateRound(request, round);
        return roundMapper.toRoundResponse(roundRepository.save(round));
    }

    @Override
    @Transactional
    public void delete(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        if (round.getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (!raceRepository.findByRound_RoundId(roundId).isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        roundRepository.delete(round);
    }

    @Override
    public List<RoundResponse> getRoundsByTournamentId(UUID tournamentId) {
        return roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId)
                .stream()
                .map(roundMapper::toRoundResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoundResponse assignHeadReferee(UUID roundId, UUID refereeId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));
        validateHeadRefereeAssignmentEditable(round);

        Referee referee = refereeRepository.findById(refereeId)
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (referee.getStatus() == RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.REFEREE_NOT_AVAILABLE);
        }
        if (raceRefereeRepository.existsByRace_Round_RoundIdAndReferee_RefereeId(
                roundId, refereeId)) {
            throw new AppException(ErrorCode.REFEREE_ROLE_CONFLICT_IN_ROUND);
        }

        round.setHeadReferee(referee);
        round.setHeadRefereeAssignedAt(LocalDateTime.now());

        return roundMapper.toRoundResponse(roundRepository.save(round));
    }

    @Override
    @Transactional
    public RoundResponse removeHeadReferee(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));
        validateHeadRefereeAssignmentEditable(round);

        round.setHeadReferee(null);
        round.setHeadRefereeAssignedAt(null);

        return roundMapper.toRoundResponse(roundRepository.save(round));
    }

    private void validateHeadRefereeAssignmentEditable(Round round) {
        Tournament tournament = round.getTournament();
        boolean tournamentEditable = tournament.getStatus() == TournamentStatus.DRAFT
                || tournament.getPhase() == TournamentPhase.SCHEDULING;
        if (!tournamentEditable || round.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.HEAD_REFEREE_ASSIGNMENT_LOCKED);
        }
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateOnlyRoundScheduleChanges(UpdateRoundRequest request) {
        if (request.getRoundName() != null
                || request.getSequenceOrder() != null
                || request.getIsFinal() != null
                || request.getPredictionType() != null
                || request.getAdvancementRule() != null
                || request.getDescription() != null
                || request.getMaxRaces() != null
                || request.getMaxEntries() != null
                || request.getMinEntries() != null
                || request.getQualifiersPerRace() != null
                || request.getStatus() != null
                || request.getHeadRefereeId() != null) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
    }

    private void validatePredictionType(PredictionType predictionType) {
        if (predictionType != PredictionType.TOP3) {
            throw new AppException(ErrorCode.INVALID_PREDICTION_TYPE);
        }
    }
}
