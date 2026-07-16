package com.swp391.horseracing.simulation;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.simulation.api.IncidentReviewRequest;
import com.swp391.horseracing.simulation.domain.*;
import com.swp391.horseracing.simulation.persistence.*;
import com.swp391.horseracing.simulation.realtime.RaceIncidentService;
import com.swp391.horseracing.simulation.realtime.SimulationAccessService;
import com.swp391.horseracing.simulation.realtime.RaceControlPublisher;
import com.swp391.horseracing.repository.RaceEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaceIncidentServiceTest {
    @Mock SimulationAccessService accessService;
    @Mock RaceSimulationSessionRepository sessionRepository;
    @Mock RaceSimulationWarningRepository warningRepository;
    @Mock RaceSimulationFlagRepository flagRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceControlPublisher publisher;

    private RaceIncidentService service;
    private Race race;
    private User refereeUser;
    private RaceEntry entry;
    private RaceSimulationSession session;

    @BeforeEach
    void setUp() {
        service = new RaceIncidentService(accessService, sessionRepository, warningRepository, flagRepository, raceEntryRepository, publisher);
        UUID raceId = UUID.randomUUID();
        refereeUser = User.builder().userId(UUID.randomUUID()).fullName("Referee").build();
        Horse horse = Horse.builder().horseId(UUID.randomUUID()).name("Horse").build();
        Jockey jockey = Jockey.builder().user(User.builder().fullName("Jockey").build()).build();
        entry = RaceEntry.builder().entryId(UUID.randomUUID())
                .contract(JockeyHorseContract.builder().horse(horse).jockey(jockey).build()).build();
        race = Race.builder().raceId(raceId).build();
        entry.setRace(race);
        session = RaceSimulationSession.builder().sessionId(UUID.randomUUID()).race(race)
                .status(SimulationStatus.FINISHED).build();
        when(accessService.requireAssignedReferee(raceId))
                .thenReturn(new SimulationAccessService.AccessContext(race, refereeUser, null));
    }

    @Test
    void ignoringWarningOnlyReviewsWarningAndDoesNotCreateFlagOrViolation() {
        RaceSimulationWarning warning = warning();
        when(warningRepository.findById(warning.getWarningId())).thenReturn(Optional.of(warning));
        when(warningRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.ignore(race.getRaceId(), warning.getWarningId(), new IncidentReviewRequest("Reviewed"));

        assertEquals(WarningReviewStatus.IGNORED, response.reviewStatus());
        verifyNoInteractions(flagRepository);
    }

    @Test
    void confirmFlagReturnsViolationDraftButDoesNotChooseDangerousPenalty() {
        RaceSimulationFlag flag = RaceSimulationFlag.builder()
                .flagId(UUID.randomUUID()).session(session).race(race).entry(entry)
                .horseId(entry.getContract().getHorse().getHorseId())
                .source(FlagSource.MANUAL).status(FlagReviewStatus.PENDING_REVIEW)
                .severity(SimulationSeverity.HIGH).raceTimeSeconds(32.0)
                .note("Observed performance requires review")
                .flaggedBy(refereeUser).flaggedAt(LocalDateTime.now()).build();
        when(flagRepository.findById(flag.getFlagId())).thenReturn(Optional.of(flag));
        when(flagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.reviewFlag(race.getRaceId(), flag.getFlagId(), new IncidentReviewRequest("Confirmed"), true);

        assertEquals(FlagReviewStatus.CONFIRMED, response.status());
        assertNotNull(response.violationDraft());
        assertEquals("WARNING", response.violationDraft().suggestedPenaltyType());
        assertEquals("OTHER", response.violationDraft().suggestedType());
    }

    private RaceSimulationWarning warning() {
        return RaceSimulationWarning.builder()
                .warningId(UUID.randomUUID()).session(session).race(race).entry(entry)
                .horseId(entry.getContract().getHorse().getHorseId())
                .warningType(SimulationWarningType.ABNORMAL_SPEED_SPIKE)
                .severity(SimulationSeverity.HIGH).riskScore(.8).raceTimeSeconds(10.0)
                .sequence(20L).message("Spike").createdAt(LocalDateTime.now())
                .reviewStatus(WarningReviewStatus.PENDING).build();
    }
}
