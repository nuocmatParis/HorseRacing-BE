package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RaceDistance;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.service.impl.RaceResultServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceResultFinishWorkflowTest {

    @Mock RaceResultRepository raceResultRepository;
    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock RefereeRepository refereeRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock RaceResultMapper raceResultMapper;
    @Mock UserCurrentService userCurrentService;
    @InjectMocks RaceResultServiceImpl service;

    @Test
    void finishRacePreservesExistingManualResultAndGeneratesMissingResults() {
        UUID raceId = UUID.randomUUID();
        User refereeUser = new User();
        refereeUser.setUserId(UUID.randomUUID());
        Referee referee = new Referee();
        referee.setRefereeId(UUID.randomUUID());
        referee.setUser(refereeUser);
        referee.setStatus(RefereeStatus.AVAILABLE);

        Race race = new Race();
        race.setRaceId(raceId);
        race.setStatus(RoundStatus.ONGOING);
        race.setDistance(RaceDistance.MILE_1600M);

        RaceEntry firstEntry = new RaceEntry();
        firstEntry.setEntryId(UUID.randomUUID());
        firstEntry.setRace(race);
        firstEntry.setStatus(RaceEntryStatus.FINISHED);
        RaceEntry secondEntry = new RaceEntry();
        secondEntry.setEntryId(UUID.randomUUID());
        secondEntry.setRace(race);
        secondEntry.setStatus(RaceEntryStatus.CONFIRMED);

        RaceResult existingResult = new RaceResult();
        existingResult.setRace(race);
        existingResult.setEntry(firstEntry);
        existingResult.setStatus(RaceResultStatus.FINISHED);
        existingResult.setRank(1);
        existingResult.setFinishTime(90F);

        when(raceRepository.findForUpdateByRaceId(raceId)).thenReturn(Optional.of(race));
        when(userCurrentService.getCurrentUser()).thenReturn(refereeUser);
        when(refereeRepository.findByUser_UserId(refereeUser.getUserId())).thenReturn(Optional.of(referee));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(raceId, referee.getRefereeId()))
                .thenReturn(true);
        when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId))
                .thenReturn(List.of(firstEntry, secondEntry));
        when(raceResultRepository.findByRace_RaceId(raceId)).thenReturn(List.of(existingResult));
        when(raceResultRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.finishRaceWithRandomResults(raceId);

        assertEquals(RoundStatus.FINISHED, race.getStatus());
        assertEquals(RaceEntryStatus.FINISHED, secondEntry.getStatus());
    }
}
