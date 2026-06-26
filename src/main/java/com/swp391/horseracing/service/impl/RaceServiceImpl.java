package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceMapper;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.RaceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceServiceImpl implements RaceService {

    RaceRepository raceRepository;
    RoundRepository roundRepository;
    UserRepository userRepository;
    RaceMapper raceMapper;

    @Override
    @Transactional
    public RaceResponse create(UUID roundId, CreateRaceRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_RACE_DATES);
        }

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        if (round.getTournament().getStatus() != TournamentStatus.DRAFT) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_IN_DRAFT);
        }

        if (request.getPredictionOpenAt().isAfter(request.getPredictionCloseAt())
                || request.getPredictionCloseAt().isAfter(request.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_PREDICTION_TIMES);
        }

        if (request.getStartTime().isBefore(round.getStartDate())
                || request.getEndTime().isAfter(round.getEndDate())) {
            throw new AppException(ErrorCode.RACE_DATES_OUT_OF_ROUND);
        }

        if (raceRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.RACE_NAME_ALREADY_EXISTS);
        }

        User currentUser = getCurrentUser();

        Race race = raceMapper.toRace(request);
        race.setRound(round);
        race.setCreatedBy(currentUser);

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    @Override
    public List<RaceResponse> getRacesByRoundId(UUID roundId) {
        return raceRepository.findByRound_RoundId(roundId)
                .stream()
                .map(raceMapper::toRaceResponse)
                .toList();
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
