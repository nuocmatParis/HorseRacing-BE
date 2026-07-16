package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.*;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(raceRepository.findById(fixture.race.getRaceId())).thenReturn(Optional.of(fixture.race));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                fixture.race.getRaceId(), fixture.referee.getRefereeId())).thenReturn(true);
        when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(fixture.race.getRaceId()))
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
        when(horseInspectionRepository.findByRaceEntry_EntryId(fixture.entry.getEntryId()))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> service.startRace(fixture.race.getRaceId()));

        assertEquals(ErrorCode.ENTRY_MISSING_HORSE_INSPECTION, exception.getErrorCode());
    }

    private Fixture fixture() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        Referee referee = new Referee();
        referee.setRefereeId(UUID.randomUUID());
        referee.setUser(user);
        Tournament tournament = new Tournament();
        tournament.setStartEarlyToleranceMinutes(30);
        tournament.setStartLateToleranceMinutes(30);
        Round round = new Round();
        round.setRoundId(UUID.randomUUID());
        round.setTournament(tournament);
        round.setMinEntries(1);
        Race race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setRound(round);
        race.setStatus(RoundStatus.SCHEDULED);
        race.setStartTime(LocalDateTime.now());
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
