package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_referee.request.CreateRaceRefereeRequest;
import com.swp391.horseracing.dto.race_referee.response.RaceRefereeResponse;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceReferee;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceRefereeMapper;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.service.impl.RaceRefereeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceRefereeSingleAssignmentTest {

    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock RaceRepository raceRepository;
    @Mock RefereeRepository refereeRepository;
    @Mock RaceRefereeMapper raceRefereeMapper;
    @Mock UserCurrentService userCurrentService;

    @InjectMocks RaceRefereeServiceImpl raceRefereeService;

    @Test
    void createRejectsAnotherDirectRefereeWhenRaceAlreadyHasOne() {
        UUID raceId = UUID.randomUUID();
        UUID refereeId = UUID.randomUUID();
        CreateRaceRefereeRequest request = CreateRaceRefereeRequest.builder()
                .raceId(raceId)
                .refereeId(refereeId)
                .build();
        Race race = schedulingRace(raceId);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceRefereeRepository.countByRace_RaceId(raceId)).thenReturn(1);

        AppException exception = assertThrows(
                AppException.class,
                () -> raceRefereeService.create(request));

        assertEquals(ErrorCode.RACE_REQUIRES_EXACTLY_ONE_REFEREE, exception.getErrorCode());
        verify(refereeRepository, never()).findById(refereeId);
        verify(raceRefereeRepository, never()).save(org.mockito.ArgumentMatchers.any(RaceReferee.class));
    }

    @Test
    void createAssignsFirstDirectReferee() {
        UUID raceId = UUID.randomUUID();
        UUID refereeId = UUID.randomUUID();
        CreateRaceRefereeRequest request = CreateRaceRefereeRequest.builder()
                .raceId(raceId)
                .refereeId(refereeId)
                .build();
        Race race = schedulingRace(raceId);
        Referee referee = new Referee();
        referee.setRefereeId(refereeId);
        referee.setStatus(RefereeStatus.AVAILABLE);
        User assignedBy = new User();
        RaceReferee assignment = new RaceReferee();
        RaceRefereeResponse expectedResponse = new RaceRefereeResponse();

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceRefereeRepository.countByRace_RaceId(raceId)).thenReturn(0);
        when(refereeRepository.findById(refereeId)).thenReturn(Optional.of(referee));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(raceId, refereeId))
                .thenReturn(false);
        when(userCurrentService.getCurrentUser()).thenReturn(assignedBy);
        when(raceRefereeMapper.toRaceReferee(request)).thenReturn(assignment);
        when(raceRefereeRepository.save(assignment)).thenReturn(assignment);
        when(raceRefereeMapper.toRaceRefereeResponse(assignment)).thenReturn(expectedResponse);

        RaceRefereeResponse actualResponse = raceRefereeService.create(request);

        assertSame(expectedResponse, actualResponse);
        assertSame(race, assignment.getRace());
        assertSame(referee, assignment.getReferee());
        assertSame(assignedBy, assignment.getAssignedBy());
        assertNotNull(assignment.getAssignedAt());
        verify(raceRefereeRepository).save(assignment);
    }

    private Race schedulingRace(UUID raceId) {
        Round round = new Round();
        Race race = new Race();
        race.setRaceId(raceId);
        race.setRound(round);
        race.setStatus(RoundStatus.SCHEDULING);
        return race;
    }
}
