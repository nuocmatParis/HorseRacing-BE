package com.swp391.horseracing.simulation;

import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.dto.race.response.RaceStartReadinessResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.RaceService;
import com.swp391.horseracing.simulation.anomaly.DeterministicAnomalyDetector;
import com.swp391.horseracing.simulation.domain.SimulationStatus;
import com.swp391.horseracing.simulation.engine.*;
import com.swp391.horseracing.simulation.mapper.SimulationProfileMapper;
import com.swp391.horseracing.simulation.persistence.*;
import com.swp391.horseracing.simulation.realtime.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaceSimulationLifecycleServiceTest {
    @Mock RaceService raceService;
    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock HorseInspectionRepository horseInspectionRepository;
    @Mock RaceSimulationSessionRepository sessionRepository;
    @Mock RaceSimulationParticipantRepository participantRepository;
    @Mock SimulationProfileMapper profileMapper;
    @Mock SimulationAccessService accessService;
    @Mock RaceControlPublisher publisher;

    private RaceSimulationLifecycleService service;
    private Race race;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RaceSimulationLifecycleService(
                raceService,
                raceRepository,
                raceEntryRepository,
                horseInspectionRepository,
                sessionRepository,
                participantRepository,
                profileMapper,
                new DeterministicRaceEngine(new DeterministicAnomalyDetector()),
                accessService,
                new ObjectMapper(),
                publisher);
        user = User.builder().userId(UUID.randomUUID()).build();
        Tournament tournament = Tournament.builder().name("Cup").build();
        Round round = Round.builder().tournament(tournament).minEntries(2).build();
        race = Race.builder()
                .raceId(UUID.randomUUID())
                .name("Race 1")
                .distance(RaceDistance.SPRINT_1000M)
                .status(RoundStatus.SCHEDULED)
                .round(round)
                .build();
        when(accessService.requireAssignedReferee(race.getRaceId()))
                .thenReturn(new SimulationAccessService.AccessContext(race, user, null));
    }

    @Test
    void prepareSnapshotsOnlyActiveEntriesAndExcludesScratchedEntry() {
        RaceEntry active = entry(RaceEntryStatus.CONFIRMED, 1);
        RaceEntry scratched = entry(RaceEntryStatus.SCRATCHED, 2);
        SimulationProfile profile = profile(active, 1);
        when(raceService.getStartReadiness(race.getRaceId()))
                .thenReturn(RaceStartReadinessResponse.builder().canStart(true).build());
        when(raceRepository.findForUpdateByRaceId(race.getRaceId())).thenReturn(Optional.of(race));
        when(sessionRepository.findForUpdateByRaceId(race.getRaceId())).thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenAnswer(invocation -> {
            RaceSimulationSession session = invocation.getArgument(0);
            if (session.getSessionId() == null) session.setSessionId(UUID.randomUUID());
            return session;
        });
        when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(race.getRaceId()))
                .thenReturn(List.of(active, scratched));
        when(horseInspectionRepository.findByRaceEntry_EntryId(active.getEntryId()))
                .thenReturn(Optional.empty());
        when(profileMapper.map(active, null)).thenReturn(profile);
        when(participantRepository.findBySession_SessionIdOrderByLaneNumberAsc(any()))
                .thenReturn(List.of());

        service.prepare(race.getRaceId());

        verify(profileMapper).map(active, null);
        verify(profileMapper, never()).map(eq(scratched), any());
        verify(participantRepository, times(1)).save(any());
    }

    @Test
    void repeatedPrepareReturnsExistingReadySessionInsteadOfCreatingSecondSession() {
        RaceSimulationSession existing = RaceSimulationSession.builder()
                .sessionId(UUID.randomUUID())
                .race(race)
                .status(SimulationStatus.READY)
                .randomSeed(1L)
                .currentSequence(0L)
                .currentRaceTimeSeconds(0.0)
                .preparedBy(user)
                .build();
        when(raceService.getStartReadiness(race.getRaceId()))
                .thenReturn(RaceStartReadinessResponse.builder().canStart(true).build());
        when(raceRepository.findForUpdateByRaceId(race.getRaceId())).thenReturn(Optional.of(race));
        when(sessionRepository.findForUpdateByRaceId(race.getRaceId())).thenReturn(Optional.of(existing));
        when(participantRepository.findBySession_SessionIdOrderByLaneNumberAsc(existing.getSessionId()))
                .thenReturn(List.of());

        var response = service.prepare(race.getRaceId());

        assertEquals(existing.getSessionId(), response.sessionId());
        verify(sessionRepository, never()).save(any());
        verifyNoInteractions(profileMapper);
    }

    private RaceEntry entry(RaceEntryStatus status, int lane) {
        Horse horse = Horse.builder().horseId(UUID.randomUUID()).name("Horse " + lane).build();
        User jockeyUser = User.builder().fullName("Jockey " + lane).build();
        Jockey jockey = Jockey.builder().jockeyId(UUID.randomUUID()).user(jockeyUser).build();
        JockeyHorseContract contract = JockeyHorseContract.builder().horse(horse).jockey(jockey).build();
        return RaceEntry.builder().entryId(UUID.randomUUID()).race(race).contract(contract)
                .laneNumber(lane).status(status).build();
    }

    private SimulationProfile profile(RaceEntry entry, int lane) {
        return new SimulationProfile(
                entry.getEntryId(), entry.getContract().getHorse().getHorseId(),
                entry.getContract().getHorse().getName(), null,
                entry.getContract().getJockey().getJockeyId(),
                entry.getContract().getJockey().getUser().getFullName(), lane,
                15, 3, .8, .8, .8, .6, .8, .8, null);
    }
}
