package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.impl.ScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionScoringDetailTest {

    @Mock
    private PredictionRepository predictionRepository;
    @Mock
    private RaceResultRepository raceResultRepository;
    @Mock
    private SpectatorRepository spectatorRepository;
    @Mock
    private BusinessNotificationEventService notificationEventService;

    @InjectMocks
    private ScoringServiceImpl scoringService;

    private UUID raceId;
    private Tournament tournament;
    private Race race;
    private Spectator spectator;

    @BeforeEach
    void setUp() {
        raceId = UUID.randomUUID();

        tournament = Tournament.builder()
                .tournamentId(UUID.randomUUID())
                .predictionTop1CorrectPoints(50)
                .predictionTop3ExactPositionPoints(30)
                .predictionTop3CorrectHorsePoints(10)
                .predictionTop3PerfectBonusPoints(20)
                .build();

        Round round = Round.builder()
                .roundId(UUID.randomUUID())
                .tournament(tournament)
                .build();

        race = Race.builder()
                .raceId(raceId)
                .round(round)
                .build();

        spectator = Spectator.builder()
                .spectatorId(UUID.randomUUID())
                .totalPoints(100)
                .build();
    }

    @Test
    @DisplayName("Chấm điểm phân biệt CORRECT_EXACT_POSITION (Đúng vị trí) và CORRECT_IN_TOP3 (Đúng trong Top 3)")
    void testScoreRace_DistinguishesExactPositionAndInTop3() {
        UUID entry1Id = UUID.randomUUID();
        UUID entry2Id = UUID.randomUUID();

        RaceEntry entry1 = RaceEntry.builder().entryId(entry1Id).build();
        RaceEntry entry2 = RaceEntry.builder().entryId(entry2Id).build();

        PredictionDetail detail1 = PredictionDetail.builder()
                .entry(entry1)
                .predictedRank(1)
                .status(PredictionDetailStatus.UNSCORED)
                .build();

        PredictionDetail detail2 = PredictionDetail.builder()
                .entry(entry2)
                .predictedRank(2)
                .status(PredictionDetailStatus.UNSCORED)
                .build();

        Prediction prediction = Prediction.builder()
                .predictionId(UUID.randomUUID())
                .race(race)
                .spectator(spectator)
                .predictionType(PredictionType.TOP3)
                .status(PredictionStatus.PENDING)
                .predictionDetails(new ArrayList<>(List.of(detail1, detail2)))
                .build();

        // Race Result: Entry1 finished Rank 1 (Exact), Entry2 finished Rank 3 (In Top 3 but not exact rank 2)
        RaceResult result1 = RaceResult.builder()
                .entry(entry1)
                .rank(1)
                .status(RaceResultStatus.FINISHED)
                .build();

        RaceResult result2 = RaceResult.builder()
                .entry(entry2)
                .rank(3)
                .status(RaceResultStatus.FINISHED)
                .build();

        when(predictionRepository.findByRace_RaceIdAndStatus(raceId, PredictionStatus.PENDING))
                .thenReturn(List.of(prediction));
        when(raceResultRepository.findByRace_RaceId(raceId))
                .thenReturn(List.of(result1, result2));

        scoringService.scoreRace(raceId);

        // Detail 1 (Predicted 1, Actual 1) -> CORRECT_EXACT_POSITION (30 pts)
        assertEquals(PredictionDetailStatus.CORRECT_EXACT_POSITION, detail1.getStatus());
        assertEquals(30, detail1.getAwardedPoints());

        // Detail 2 (Predicted 2, Actual 3) -> CORRECT_IN_TOP3 (10 pts)
        assertEquals(PredictionDetailStatus.CORRECT_IN_TOP3, detail2.getStatus());
        assertEquals(10, detail2.getAwardedPoints());

        // Total reward points = 30 + 10 = 40
        assertEquals(40, prediction.getRewardPoints());
        assertEquals(PredictionStatus.SCORED, prediction.getStatus());

        // Spectator total points updated from 100 to 140
        assertEquals(140, spectator.getTotalPoints());
        verify(spectatorRepository).save(spectator);
    }
}
