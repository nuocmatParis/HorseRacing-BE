package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse;
import com.swp391.horseracing.dto.race_portal.RaceSummaryResponse;
import com.swp391.horseracing.entity.AIPrediction;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.JockeyTier;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.AIPredictionPublicationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.repository.AIPredictionRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.JockeyRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.impl.HomePageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomePageServiceImplTest {

    @Mock TournamentRepository tournamentRepository;
    @Mock HorseRepository horseRepository;
    @Mock JockeyRepository jockeyRepository;
    @Mock RaceRepository raceRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock RaceResultRepository raceResultRepository;
    @Mock AIPredictionRepository aiPredictionRepository;
    @Mock RacePortalService racePortalService;

    @InjectMocks HomePageServiceImpl homePageService;

    @Test
    void getHomePageAggregatesOnlyHomepageData() {
        LocalDateTime now = LocalDateTime.now();
        UUID tournamentId = UUID.randomUUID();
        UUID roundId = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID horseId = UUID.randomUUID();
        UUID jockeyId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        RaceSummaryResponse raceSummary = RaceSummaryResponse.builder()
                .tournamentId(tournamentId)
                .tournamentName("Cúp Mùa Hè")
                .roundId(roundId)
                .roundName("Chung kết")
                .raceId(raceId)
                .raceName("Trận chung kết")
                .startTime(now.plusMinutes(30))
                .endTime(now.plusMinutes(40))
                .trackCondition("GOOD")
                .distance(com.swp391.horseracing.enums.RaceDistance.MILE_1600M)
                .sequenceOrder(1)
                .status(RoundStatus.SCHEDULED)
                .predictionOpenAt(now.minusMinutes(30))
                .predictionCloseAt(now.plusMinutes(25))
                .build();
        PageResponse<RaceSummaryResponse> racePage = new PageResponse<>(
                List.of(raceSummary), 0, 20, 1, 1, true, true);
        when(racePortalService.getUpcomingRaces(
                any(LocalDateTime.class), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(racePage);
        when(racePortalService.getUpcomingRaces(
                any(LocalDateTime.class), any(LocalDateTime.class), isNull(), anyInt(), anyInt()))
                .thenReturn(racePage);

        when(tournamentRepository.count()).thenReturn(12L);
        when(horseRepository.count()).thenReturn(48L);
        when(jockeyRepository.count()).thenReturn(21L);
        when(raceRepository.count()).thenReturn(96L);

        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setName("Cúp Mùa Hè");
        tournament.setDescription("Giải đua nổi bật trong mùa hè.");
        tournament.setStartDate(LocalDate.now());
        tournament.setEndDate(LocalDate.now().plusDays(2));
        tournament.setLocation("Trường đua Phú Thọ");
        tournament.setTotalPrizePool(new BigDecimal("500000000"));
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setPhase(TournamentPhase.REGISTRATION_OPEN);
        tournament.setCurrentRoundName("Chung kết");
        tournament.setPublishedAt(now.minusDays(1));
        when(tournamentRepository.findAll()).thenReturn(List.of(tournament));

        User jockeyUser = new User();
        jockeyUser.setFullName("Nguyễn Minh");
        jockeyUser.setImageUrl("/images/jockey-minh.jpg");
        Jockey jockey = new Jockey();
        jockey.setJockeyId(jockeyId);
        jockey.setUser(jockeyUser);
        jockey.setTotalRaces(20);
        jockey.setTotalWins(5);
        jockey.setJockeyTier(JockeyTier.PROFESSIONAL);

        Horse horse = new Horse();
        horse.setHorseId(horseId);
        horse.setName("Sấm Sét");
        horse.setImageUrl("/images/sam-set.jpg");
        horse.setCurrentRating(1280);
        horse.setHighestRating(1310);
        horse.setTotalRaces(18);
        horse.setTotalWins(6);
        horse.setTotalTop3Finishes(12);
        horse.setWinRate(33.33);

        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setHorse(horse);
        contract.setJockey(jockey);
        RaceEntry entry = new RaceEntry();
        entry.setEntryId(entryId);
        entry.setLaneNumber(3);
        entry.setContract(contract);

        Round round = new Round();
        round.setRoundId(roundId);
        round.setTournament(tournament);
        Race race = new Race();
        race.setRaceId(raceId);
        race.setName("Trận chung kết");
        race.setStartTime(now.minusHours(2));
        race.setFinishedAt(now.minusHours(1));
        race.setRound(round);
        race.setAiPredictionPublicationStatus(AIPredictionPublicationStatus.PUBLISHED);
        entry.setRace(race);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));

        RaceReport report = new RaceReport();
        report.setRace(race);
        report.setStatus(ReportStatus.PUBLISHED);
        report.setPublishedAt(now.minusMinutes(30));
        when(raceReportRepository.findAll()).thenReturn(List.of(report));

        RaceResult result = new RaceResult();
        result.setResultId(UUID.randomUUID());
        result.setEntry(entry);
        result.setRace(race);
        result.setRank(1);
        result.setFinishTime(95.42F);
        result.setStatus(RaceResultStatus.FINISHED);
        when(raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId))
                .thenReturn(List.of(result));

        when(horseRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(horse)));
        when(jockeyRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(jockey)));

        AIPrediction prediction = new AIPrediction();
        prediction.setPredictionId(UUID.randomUUID());
        prediction.setEntry(entry);
        prediction.setWinProbability(new BigDecimal("62.50"));
        prediction.setConfidenceScore(new BigDecimal("84.00"));
        prediction.setPredictedTopN(3);
        prediction.setPredictionReason("Phong độ và rating ổn định.");
        prediction.setGeneratedAt(now.minusMinutes(10));
        when(aiPredictionRepository.findByEntry_Race_RaceIdOrderByCreatedAtAsc(raceId))
                .thenReturn(List.of(prediction));

        HomePageResponse response = homePageService.getHomePage();

        assertEquals(12L, response.stats().tournamentCount());
        assertEquals(48L, response.stats().horseCount());
        assertEquals("Cúp Mùa Hè", response.featuredTournament().name());
        assertEquals(1, response.upcomingRaces().size());
        assertEquals(1, response.todaySchedule().size());
        assertEquals("Sấm Sét", response.latestResults().results().getFirst().horseName());
        assertEquals(1280, response.horseLeaderboard().getFirst().currentRating());
        assertEquals(new BigDecimal("25.00"), response.jockeyLeaderboard().getFirst().winRate());
        assertEquals(new BigDecimal("62.50"),
                response.featuredPrediction().candidates().getFirst().winProbability());
        assertNotNull(response.generatedAt());
    }

    @Test
    void getHomePageReturnsEmptyOptionalSectionsWhenNoPublicContentExists() {
        PageResponse<RaceSummaryResponse> emptyPage = new PageResponse<>(
                Collections.emptyList(), 0, 20, 0, 0, true, true);
        when(racePortalService.getUpcomingRaces(
                any(LocalDateTime.class), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(emptyPage);
        when(racePortalService.getUpcomingRaces(
                any(LocalDateTime.class), any(LocalDateTime.class), isNull(), anyInt(), anyInt()))
                .thenReturn(emptyPage);
        when(tournamentRepository.findAll()).thenReturn(Collections.emptyList());
        when(raceReportRepository.findAll()).thenReturn(Collections.emptyList());
        when(horseRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(jockeyRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        HomePageResponse response = homePageService.getHomePage();

        assertNull(response.featuredTournament());
        assertNull(response.latestResults());
        assertNull(response.featuredPrediction());
        assertEquals(Collections.emptyList(), response.upcomingRaces());
        assertEquals(Collections.emptyList(), response.todaySchedule());
    }
}
