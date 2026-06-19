package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
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

        User currentUser = getCurrentUser();

        Tournament tournament = tournamentMapper.toTournament(request);
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPhase(TournamentPhase.REGISTRATION);
        tournament.setCreatedBy(currentUser);
        tournament.setCreatedAt(LocalDateTime.now());

        return tournamentMapper.toTournamentResponse(tournamentRepository.save(tournament));
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

        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setPublishedAt(LocalDateTime.now());
        return tournamentMapper.toTournamentResponse(tournamentRepository.save(tournament));
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
                .map(tournamentMapper::toTournamentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TournamentResponse getById(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));
        return tournamentMapper.toTournamentResponse(tournament);
    }
}
