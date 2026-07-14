package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_portal.SpectatorRaceDetailResponse;
import com.swp391.horseracing.entity.AIPrediction;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.RaceDistance;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.repository.AIPredictionRepository;
import com.swp391.horseracing.repository.HorseInspectionRepository;
import com.swp391.horseracing.repository.JockeyInspectionRepository;
import com.swp391.horseracing.repository.MedicalStaffRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceInspectionStaffAssignmentRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.VeterinarianRepository;
import com.swp391.horseracing.service.impl.RacePortalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RacePortalServiceImplTest {

    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock RaceInspectionStaffAssignmentRepository inspectionAssignmentRepository;
    @Mock RefereeRepository refereeRepository;
    @Mock VeterinarianRepository veterinarianRepository;
    @Mock MedicalStaffRepository medicalStaffRepository;
    @Mock TournamentRepository tournamentRepository;
    @Mock UserCurrentService userCurrentService;
    @Mock HorseInspectionRepository horseInspectionRepository;
    @Mock JockeyInspectionRepository jockeyInspectionRepository;
    @Mock AIPredictionRepository aiPredictionRepository;

    @InjectMocks RacePortalServiceImpl racePortalService;

    private UUID raceId;
    private Race race;
    private RaceEntry entry;

    @BeforeEach
    void setUp() {
        raceId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .tournamentId(UUID.randomUUID())
                .name("Giải mùa hè")
                .build();
        Round round = Round.builder()
                .roundId(UUID.randomUUID())
                .roundName("Chung kết")
                .tournament(tournament)
                .build();
        race = Race.builder()
                .raceId(raceId)
                .name("Cuộc đua chung kết")
                .round(round)
                .distance(RaceDistance.SPRINT_1200M)
                .trackCondition("GOOD")
                .status(RoundStatus.SCHEDULED)
                .schedulePublishedAt(LocalDateTime.now())
                .build();

        User jockeyUser = User.builder().userId(UUID.randomUUID()).fullName("Kỵ sĩ An").build();
        Jockey jockey = Jockey.builder().jockeyId(UUID.randomUUID()).user(jockeyUser).build();
        HorseOwner owner = HorseOwner.builder()
                .ownerId(UUID.randomUUID())
                .user(User.builder().userId(UUID.randomUUID()).fullName("Chủ ngựa Bình").build())
                .build();
        Horse horse = Horse.builder().horseId(UUID.randomUUID()).name("Sao Mai").owner(owner).build();
        JockeyHorseContract contract = JockeyHorseContract.builder()
                .contractId(UUID.randomUUID())
                .tournament(tournament)
                .owner(owner)
                .horse(horse)
                .jockey(jockey)
                .build();
        entry = RaceEntry.builder()
                .entryId(UUID.randomUUID())
                .race(race)
                .contract(contract)
                .laneNumber(3)
                .build();

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId))
                .thenReturn(List.of(entry));
        when(horseInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId()))
                .thenReturn(Optional.empty());
        when(jockeyInspectionRepository.findByRaceEntry_EntryId(entry.getEntryId()))
                .thenReturn(Optional.empty());
    }

    @Test
    void spectatorDetailMergesExistingAiPrediction() {
        AIPrediction prediction = AIPrediction.builder()
                .entry(entry)
                .winProbability(new BigDecimal("42.50"))
                .topNProbability(new BigDecimal("71.25"))
                .confidenceScore(new BigDecimal("88.00"))
                .predictionReason("Phong độ gần đây ổn định")
                .build();
        when(aiPredictionRepository.findByEntry_Race_RaceId(raceId))
                .thenReturn(List.of(prediction));

        SpectatorRaceDetailResponse response = racePortalService.getSpectatorRaceDetail(raceId);

        assertEquals(new BigDecimal("42.50"), response.getEntries().get(0).getWinProbability());
        assertEquals(new BigDecimal("71.25"), response.getEntries().get(0).getTopNProbability());
        assertEquals(new BigDecimal("88.00"), response.getEntries().get(0).getConfidenceScore());
        assertEquals("Phong độ gần đây ổn định", response.getEntries().get(0).getPredictionReason());
        verify(aiPredictionRepository).findByEntry_Race_RaceId(raceId);
    }

    @Test
    void spectatorDetailKeepsProbabilityNullWhenPredictionIsNotGenerated() {
        when(aiPredictionRepository.findByEntry_Race_RaceId(raceId)).thenReturn(List.of());

        SpectatorRaceDetailResponse response = racePortalService.getSpectatorRaceDetail(raceId);

        assertNull(response.getEntries().get(0).getWinProbability());
        assertNull(response.getEntries().get(0).getTopNProbability());
        assertNull(response.getEntries().get(0).getConfidenceScore());
        assertNull(response.getEntries().get(0).getPredictionReason());
        verify(aiPredictionRepository).findByEntry_Race_RaceId(raceId);
    }
}
