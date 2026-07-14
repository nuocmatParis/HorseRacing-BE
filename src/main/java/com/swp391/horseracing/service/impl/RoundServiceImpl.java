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
import com.swp391.horseracing.enums.BracketPlanStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RoundMapper;
import com.swp391.horseracing.repository.RefereeRepository;
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
    RoundMapper roundMapper;

    @Override
    @Transactional
    public RoundResponse create(UUID tournamentId, CreateRoundRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_ROUND_DATES);
        }

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
        if (tournament.getBracketPlanStatus() != BracketPlanStatus.NOT_GENERATED) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
        }

        if (roundRepository.existsByTournament_TournamentIdAndSequenceOrder(tournamentId, request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_ROUND_SEQUENCE);
        }

        if (roundRepository.existsByTournament_TournamentIdAndRoundName(tournamentId, request.getRoundName())) {
            throw new AppException(ErrorCode.ROUND_NAME_ALREADY_EXISTS);
        }

        List<Round> existingRounds = roundRepository
                .findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId);

        if (existingRounds.size() >= tournament.getMaxRounds()) {
            throw new AppException(ErrorCode.MAX_ROUNDS_REACHED);
        }

        if (!existingRounds.isEmpty()) {
            Round lastRound = existingRounds.get(existingRounds.size() - 1);
            if (request.getStartDate().isBefore(lastRound.getEndDate().plusDays(tournament.getMinRoundGapDays()))) {
                throw new AppException(ErrorCode.ROUND_GAP_TOO_SHORT);
            }
        }

        if (request.getStartDate().toLocalDate().isBefore(tournament.getStartDate())
                || request.getEndDate().toLocalDate().isAfter(tournament.getEndDate())) {
            throw new AppException(ErrorCode.ROUND_DATES_OUT_OF_TOURNAMENT);
        }

        User currentUser = getCurrentUser();

        Round round = roundMapper.toRound(request);
        if (existingRounds.size() + 1 == tournament.getMaxRounds()) {
            round.setFinal(true);
        }
        round.setTournament(tournament);
        round.setCreatedBy(currentUser);
        round.setCreatedAt(LocalDateTime.now());

        Round saved = roundRepository.save(round);

        if (request.getHeadRefereeId() != null) {
            Referee referee = refereeRepository.findById(request.getHeadRefereeId())
                    .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));
            saved.setHeadReferee(referee);
            saved.setHeadRefereeAssignedAt(LocalDateTime.now());
            saved = roundRepository.save(saved);
        }

        return roundMapper.toRoundResponse(saved);
    }

    @Override
    @Transactional
    public RoundResponse update(UUID roundId, UpdateRoundRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        Tournament tournament = round.getTournament();
        boolean scheduleEditable = tournament.getStatus() == TournamentStatus.DRAFT
                || tournament.getPhase() == TournamentPhase.SCHEDULING;
        if (!scheduleEditable) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }
        if (tournament.getBracketPlanStatus() == BracketPlanStatus.CONFIRMED
                || tournament.getBracketPlanStatus() == BracketPlanStatus.LOCKED) {
            validateOnlyRoundScheduleChanges(request);
        }

        if (request.getRoundName() != null && !request.getRoundName().equals(round.getRoundName())
                && roundRepository.existsByTournament_TournamentIdAndRoundName(tournament.getTournamentId(), request.getRoundName())) {
            throw new AppException(ErrorCode.ROUND_NAME_ALREADY_EXISTS);
        }

        if (request.getSequenceOrder() != null && request.getSequenceOrder() != round.getSequenceOrder()
                && roundRepository.existsByTournament_TournamentIdAndSequenceOrder(tournament.getTournamentId(), request.getSequenceOrder())) {
            throw new AppException(ErrorCode.DUPLICATE_ROUND_SEQUENCE);
        }

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_ROUND_DATES);
        }

        LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : round.getStartDate();
        LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : round.getEndDate();

        if ((request.getStartDate() != null || request.getEndDate() != null)
                && (startDate == null || endDate == null || endDate.isBefore(startDate))) {
            throw new AppException(ErrorCode.INVALID_ROUND_DATES);
        }

        if (startDate != null && endDate != null
                && (startDate.toLocalDate().isBefore(tournament.getStartDate())
                || endDate.toLocalDate().isAfter(tournament.getEndDate()))) {
            throw new AppException(ErrorCode.ROUND_DATES_OUT_OF_TOURNAMENT);
        }

        int gapDays = tournament.getMinRoundGapDays();
        List<Round> allRounds = roundRepository
                .findByTournament_TournamentIdOrderBySequenceOrderAsc(tournament.getTournamentId());
        for (Round r : allRounds) {
            if (r.getRoundId().equals(round.getRoundId())) continue;
            if (startDate == null || endDate == null || r.getStartDate() == null || r.getEndDate() == null) continue;
            if (r.getSequenceOrder() < round.getSequenceOrder()
                    && startDate.isBefore(r.getEndDate().plusDays(gapDays))) {
                throw new AppException(ErrorCode.ROUND_GAP_TOO_SHORT);
            }
            if (r.getSequenceOrder() > round.getSequenceOrder()
                    && r.getStartDate().isBefore(endDate.plusDays(gapDays))) {
                throw new AppException(ErrorCode.ROUND_GAP_TOO_SHORT);
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
        if (round.getTournament().getBracketPlanStatus() != BracketPlanStatus.NOT_GENERATED) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
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

        Referee referee = refereeRepository.findById(refereeId)
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        round.setHeadReferee(referee);
        round.setHeadRefereeAssignedAt(LocalDateTime.now());

        return roundMapper.toRoundResponse(roundRepository.save(round));
    }

    @Override
    @Transactional
    public RoundResponse removeHeadReferee(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        round.setHeadReferee(null);
        round.setHeadRefereeAssignedAt(null);

        return roundMapper.toRoundResponse(roundRepository.save(round));
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
                || request.getStatus() != null
                || request.getHeadRefereeId() != null) {
            throw new AppException(ErrorCode.BRACKET_PLAN_LOCKED);
        }
    }
}
