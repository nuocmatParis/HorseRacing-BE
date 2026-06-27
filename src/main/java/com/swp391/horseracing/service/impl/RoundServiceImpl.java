package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateRoundRequest;
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

        if (request.getStartDate().toLocalDate().isBefore(tournament.getStartDate())
                || request.getEndDate().toLocalDate().isAfter(tournament.getEndDate())) {
            throw new AppException(ErrorCode.ROUND_DATES_OUT_OF_TOURNAMENT);
        }

        User currentUser = getCurrentUser();

        Round round = roundMapper.toRound(request);
        round.setTournament(tournament);
        round.setCreatedBy(currentUser);
        round.setCreatedAt(LocalDateTime.now());

        return roundMapper.toRoundResponse(roundRepository.save(round));
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
