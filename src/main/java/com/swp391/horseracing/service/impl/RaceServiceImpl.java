package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
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
        User currentUser = getCurrentUser();

        Race race = raceMapper.toRace(request);
        race.setRound(round);
        race.setCreatedBy(currentUser);

        return raceMapper.toRaceResponse(raceRepository.save(race));
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
