package com.swp391.horseracing.simulation.realtime;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimulationAccessService {
    private final RaceRepository raceRepository;
    private final RefereeRepository refereeRepository;
    private final RaceRefereeRepository raceRefereeRepository;
    private final UserCurrentService userCurrentService;

    public AccessContext requireAssignedReferee(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        User user = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));
        boolean authorized = race.getRound().getHeadReferee() != null
                && race.getRound().getHeadReferee().getRefereeId().equals(referee.getRefereeId());
        if (!authorized) {
            authorized = raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                    raceId, referee.getRefereeId());
        }
        if (!authorized) {
            throw new AppException(ErrorCode.REFEREE_NOT_ASSIGNED_TO_RACE);
        }
        return new AccessContext(race, user, referee);
    }

    public record AccessContext(Race race, User user, Referee referee) {
    }
}
