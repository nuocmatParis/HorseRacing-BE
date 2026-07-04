package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_referee.request.CreateRaceRefereeRequest;
import com.swp391.horseracing.dto.race_referee.response.RaceRefereeResponse;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceReferee;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceRefereeMapper;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.service.RaceRefereeService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceRefereeServiceImpl implements RaceRefereeService {

    RaceRefereeRepository raceRefereeRepository;
    RaceRepository raceRepository;
    RefereeRepository refereeRepository;
    RaceRefereeMapper raceRefereeMapper;
    UserCurrentService userCurrentService;

    @Override
    @Transactional
    public RaceRefereeResponse create(CreateRaceRefereeRequest request) {
        Race race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }
        if (race.getSchedulePublishedAt() != null) {
            throw new AppException(ErrorCode.RACE_ALREADY_PUBLISHED);
        }

        Referee referee = refereeRepository.findById(request.getRefereeId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                request.getRaceId(), request.getRefereeId())) {
            throw new AppException(ErrorCode.RACE_REFEREE_ALREADY_ASSIGNED);
        }

        User currentUser = userCurrentService.getCurrentUser();

        RaceReferee raceReferee = raceRefereeMapper.toRaceReferee(request);
        raceReferee.setRace(race);
        raceReferee.setReferee(referee);
        raceReferee.setAssignedBy(currentUser);
        raceReferee.setAssignedAt(LocalDateTime.now());

        return raceRefereeMapper.toRaceRefereeResponse(raceRefereeRepository.save(raceReferee));
    }

    @Override
    public List<RaceRefereeResponse> getRefereesByRaceId(UUID raceId) {
        return raceRefereeRepository.findByRace_RaceId(raceId)
                .stream()
                .map(raceRefereeMapper::toRaceRefereeResponse)
                .toList();
    }

    @Override
    public RaceRefereeResponse getRefereeById(UUID raceRefereeId) {
        RaceReferee raceReferee = raceRefereeRepository.findById(raceRefereeId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REFEREE_NOT_FOUND));
        return raceRefereeMapper.toRaceRefereeResponse(raceReferee);
    }

    @Override
    @Transactional
    public void delete(UUID raceRefereeId) {
        RaceReferee raceReferee = raceRefereeRepository.findById(raceRefereeId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REFEREE_NOT_FOUND));
        raceRefereeRepository.delete(raceReferee);
    }

    @Override
    @Transactional
    public void deleteByRaceAndReferee(UUID raceId, UUID refereeId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }
        if (race.getSchedulePublishedAt() != null) {
            throw new AppException(ErrorCode.RACE_ALREADY_PUBLISHED);
        }

        RaceReferee raceReferee = raceRefereeRepository
                .findByRace_RaceIdAndReferee_RefereeId(raceId, refereeId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REFEREE_NOT_FOUND));

        raceRefereeRepository.delete(raceReferee);
    }
}
