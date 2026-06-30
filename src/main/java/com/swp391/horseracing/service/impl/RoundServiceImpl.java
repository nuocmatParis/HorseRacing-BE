package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateRoundRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateRoundRequest;
import com.swp391.horseracing.dto.tournament.response.RoundResponse;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RoundMapper;
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
            if (request.getStartDate().isBefore(lastRound.getEndDate())) {
                throw new AppException(ErrorCode.ROUND_DATES_OUT_OF_TOURNAMENT);
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

        return roundMapper.toRoundResponse(roundRepository.save(round));
    }

    @Override
    @Transactional
    public RoundResponse update(UUID roundId, UpdateRoundRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        Tournament tournament = round.getTournament();
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
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

        if (startDate.toLocalDate().isBefore(tournament.getStartDate())
                || endDate.toLocalDate().isAfter(tournament.getEndDate())) {
            throw new AppException(ErrorCode.ROUND_DATES_OUT_OF_TOURNAMENT);
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

        roundRepository.delete(round);
    }

    @Override
    public List<RoundResponse> getRoundsByTournamentId(UUID tournamentId) {
        return roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId)
                .stream()
                .map(roundMapper::toRoundResponse)
                .toList();
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
