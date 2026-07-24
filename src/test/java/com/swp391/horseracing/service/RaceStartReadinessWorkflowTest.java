package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.InspectionResult;
import com.swp391.horseracing.enums.InspectionStatus;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.impl.RaceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceStartReadinessWorkflowTest {
    @Mock RaceRepository raceRepository;
    @Mock RoundRepository roundRepository;
    @Mock UserRepository userRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock RaceMapper raceMapper;
    @Mock RefereeRepository refereeRepository;
    @Mock HorseInspectionRepository horseInspectionRepository;
    @Mock JockeyInspectionRepository jockeyInspectionRepository;
    @Mock RaceInspectionStaffAssignmentRepository raceInspectionStaffAssignmentRepository;
    @Mock VeterinarianRepository veterinarianRepository;
    @Mock MedicalStaffRepository medicalStaffRepository;
    @Mock PredictionService predictionService;
    @Mock BusinessNotificationEventService notificationEventService;
    @InjectMocks RaceServiceImpl service;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("referee", "password"));
        fixture = fixture();
        when(userRepository.findByUsername("referee")).thenReturn(Optional.of(fixture.user));
        when(refereeRepository.findByUser_UserId(fixture.user.getUserId()))
                .thenReturn(Optional.of(fixture.referee));
        lenient().when(raceRepository.findById(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.race));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                fixture.race.getRaceId(), fixture.referee.getRefereeId())).thenReturn(true);
        lenient().when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(fixture.race.getRaceId()))
                .thenReturn(List.of(fixture.entry));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readinessShowsMissingHorseAndJockeyInspections() {
        when(horseInspectionRepository.findByRaceEntry_EntryId(fixture.entry.getEntryId()))
                .thenReturn(Optional.empty());
        when(jockeyInspectionRepository.findByRaceEntry_EntryId(fixture.entry.getEntryId()))
                .thenReturn(Optional.empty());

        var response = service.getStartReadiness(fixture.race.getRaceId());

        assertFalse(response.isCanStart());
        assertEquals(2, response.getEntries().get(0).getBlockingReasons().size());
    }

    @Test
    void backendStillBlocksStartWhenFrontendReadinessIsBypassed() {
        fixture.race.setInspectionFinalizedAt(LocalDateTime.now());
        when(raceRepository.findForUpdateByRaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.race));
        when(horseInspectionRepository.findByRaceEntry_EntryId(fixture.entry.getEntryId()))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> service.startRace(fixture.race.getRaceId()));

        assertEquals(ErrorCode.ENTRY_MISSING_HORSE_INSPECTION, exception.getErrorCode());
    }

    @Test
    void backendBlocksStartBeforeScheduledTime() {
        fixture.race.setStartTime(LocalDateTime.now().plusHours(1));
        fixture.race.setInspectionFinalizedAt(LocalDateTime.now());
        when(raceRepository.findForUpdateByRaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.race));

        AppException exception = assertThrows(AppException.class,
                () -> service.startRace(fixture.race.getRaceId()));

        assertEquals(ErrorCode.RACE_START_TOO_EARLY, exception.getErrorCode());
    }

    @Test
    void fiveEligibleStartersCanRunAfterThreeEntriesFailInspection() {
        fixture.race.getRound().setMinEntries(8);
        fixture.race.setInspectionFinalizedAt(LocalDateTime.now());
        List<RaceEntry> entries = createEntries(fixture.race, 5);
        when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(fixture.race.getRaceId()))
                .thenReturn(entries);
        mockPassedInspections(entries);

        var readiness = service.getStartReadiness(fixture.race.getRaceId());

        assertTrue(readiness.isCanStart());
        assertEquals(5, readiness.getActiveEntryCount());
        assertEquals(8, readiness.getMinEntries());
        assertEquals(2, readiness.getRuntimeMinEntries());

        when(raceRepository.findForUpdateByRaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.race));
        when(raceRepository.save(fixture.race)).thenReturn(fixture.race);
        service.startRace(fixture.race.getRaceId());

        assertEquals(RoundStatus.ONGOING, fixture.race.getStatus());
    }

    @Test
    void aSingleEligibleStarterCannotRun() {
        fixture.race.getRound().setMinEntries(8);
        fixture.race.setInspectionFinalizedAt(LocalDateTime.now());
        List<RaceEntry> entries = List.of(fixture.entry);
        mockPassedInspections(entries);

        var readiness = service.getStartReadiness(fixture.race.getRaceId());

        assertFalse(readiness.isCanStart());
        assertEquals(2, readiness.getRuntimeMinEntries());

        when(raceRepository.findForUpdateByRaceId(fixture.race.getRaceId()))
                .thenReturn(Optional.of(fixture.race));
        AppException exception = assertThrows(AppException.class,
                () -> service.startRace(fixture.race.getRaceId()));
        assertEquals(ErrorCode.RACE_NOT_ENOUGH_ACTIVE_ENTRIES, exception.getErrorCode());
    }

    @Test
    void nonFinalRoundKeepsEnoughStartersToProduceTopFour() {
        fixture.race.getRound().setFinal(false);
        fixture.race.getRound().setQualifiersPerRace(4);
        fixture.race.setInspectionFinalizedAt(LocalDateTime.now());
        List<RaceEntry> entries = createEntries(fixture.race, 3);
        when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(fixture.race.getRaceId()))
                .thenReturn(entries);
        mockPassedInspections(entries);

        var readiness = service.getStartReadiness(fixture.race.getRaceId());

        assertFalse(readiness.isCanStart());
        assertEquals(4, readiness.getRuntimeMinEntries());
    }

    private void mockPassedInspections(List<RaceEntry> entries) {
        for (RaceEntry entry : entries) {
            HorseInspection horseInspection = new HorseInspection();
            horseInspection.setStatus(InspectionStatus.CONFIRMED);
            horseInspection.setResult(InspectionResult.PASS);
            JockeyInspection jockeyInspection = new JockeyInspection();
            jockeyInspection.setStatus(InspectionStatus.CONFIRMED);
            jockeyInspection.setResult(InspectionResult.PASS);
            when(horseInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId()))
                    .thenReturn(Optional.of(horseInspection));
            when(jockeyInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId()))
                    .thenReturn(Optional.of(jockeyInspection));
        }
    }

    private List<RaceEntry> createEntries(Race race, int count) {
        List<RaceEntry> entries = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Horse horse = new Horse();
            horse.setName("Horse " + (index + 1));
            Jockey jockey = new Jockey();
            User jockeyUser = new User();
            jockeyUser.setFullName("Jockey " + (index + 1));
            jockey.setUser(jockeyUser);
            JockeyHorseContract contract = new JockeyHorseContract();
            contract.setHorse(horse);
            contract.setJockey(jockey);
            RaceEntry entry = new RaceEntry();
            entry.setEntryId(UUID.randomUUID());
            entry.setRace(race);
            entry.setContract(contract);
            entry.setStatus(RaceEntryStatus.CONFIRMED);
            entries.add(entry);
        }
        return entries;
    }

    private Fixture fixture() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        Referee referee = new Referee();
        referee.setRefereeId(UUID.randomUUID());
        referee.setUser(user);
        Tournament tournament = new Tournament();
        tournament.setStartLateToleranceMinutes(30);
        Round round = new Round();
        round.setRoundId(UUID.randomUUID());
        round.setTournament(tournament);
        round.setMinEntries(8);
        Race race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setRound(round);
        race.setStatus(RoundStatus.SCHEDULED);
        race.setStartTime(LocalDateTime.now().minusMinutes(1));
        Horse horse = new Horse();
        horse.setName("Sấm Sét");
        Jockey jockey = new Jockey();
        User jockeyUser = new User();
        jockeyUser.setFullName("Lê Văn Thắng");
        jockey.setUser(jockeyUser);
        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setHorse(horse);
        contract.setJockey(jockey);
        RaceEntry entry = new RaceEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setRace(race);
        entry.setContract(contract);
        entry.setStatus(RaceEntryStatus.CONFIRMED);
        return new Fixture(user, referee, race, entry);
    }

    private record Fixture(User user, Referee referee, Race race, RaceEntry entry) {
    }
}
