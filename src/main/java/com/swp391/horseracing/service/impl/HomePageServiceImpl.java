package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.FeaturedPrediction;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.FeaturedTournament;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.HomeRace;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.HomeStats;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.HorseLeaderboardItem;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.JockeyLeaderboardItem;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.LatestRaceResults;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.LatestResultItem;
import com.swp391.horseracing.dto.homepage.response.HomePageResponse.PredictionCandidate;
import com.swp391.horseracing.dto.race_portal.RaceSummaryResponse;
import com.swp391.horseracing.entity.AIPrediction;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.repository.AIPredictionRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.JockeyRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.HomePageService;
import com.swp391.horseracing.service.RacePortalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HomePageServiceImpl implements HomePageService {

    static final int UPCOMING_RACE_LIMIT = 6;
    static final int TODAY_RACE_LIMIT = 20;
    static final int LEADERBOARD_LIMIT = 5;
    static final int LATEST_RESULT_LIMIT = 5;
    static final int PREDICTION_CANDIDATE_LIMIT = 5;

    TournamentRepository tournamentRepository;
    HorseRepository horseRepository;
    JockeyRepository jockeyRepository;
    RaceRepository raceRepository;
    RaceReportRepository raceReportRepository;
    RaceResultRepository raceResultRepository;
    AIPredictionRepository aiPredictionRepository;
    RacePortalService racePortalService;

    @Override
    @Transactional(readOnly = true)
    public HomePageResponse getHomePage() {
        LocalDateTime now = LocalDateTime.now();

        List<HomeRace> upcomingRaces = racePortalService
                .getUpcomingRaces(now, null, null, 0, UPCOMING_RACE_LIMIT)
                .getItems()
                .stream()
                .map(this::toHomeRace)
                .toList();

        LocalDateTime endOfToday = now.toLocalDate().atTime(LocalTime.MAX);
        PageResponse<RaceSummaryResponse> todayPage = racePortalService
                .getUpcomingRaces(now, endOfToday, null, 0, TODAY_RACE_LIMIT);
        List<HomeRace> todaySchedule = todayPage.getItems().stream()
                .map(this::toHomeRace)
                .toList();

        return new HomePageResponse(
                getStats(),
                getFeaturedTournament().orElse(null),
                upcomingRaces,
                getLatestRaceResults().orElse(null),
                getHorseLeaderboard(),
                getJockeyLeaderboard(),
                getFeaturedPrediction(upcomingRaces).orElse(null),
                todaySchedule,
                now
        );
    }

    private HomeStats getStats() {
        return new HomeStats(
                tournamentRepository.count(),
                horseRepository.count(),
                jockeyRepository.count(),
                raceRepository.count()
        );
    }

    private Optional<FeaturedTournament> getFeaturedTournament() {
        return tournamentRepository.findAll().stream()
                .filter(tournament -> tournament.getPublishedAt() != null)
                .filter(tournament -> tournament.getStatus() == TournamentStatus.ONGOING
                        || tournament.getStatus() == TournamentStatus.OPEN)
                .sorted(Comparator.comparingInt(this::getTournamentPriority)
                        .thenComparing(Tournament::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .map(this::toFeaturedTournament);
    }

    private int getTournamentPriority(Tournament tournament) {
        return tournament.getStatus() == TournamentStatus.ONGOING ? 0 : 1;
    }

    private FeaturedTournament toFeaturedTournament(Tournament tournament) {
        return new FeaturedTournament(
                tournament.getTournamentId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getLocation(),
                tournament.getTotalPrizePool(),
                tournament.getStatus(),
                tournament.getPhase(),
                tournament.getCurrentRoundName()
        );
    }

    private Optional<LatestRaceResults> getLatestRaceResults() {
        Optional<RaceReport> latestReport = raceReportRepository.findAll().stream()
                .filter(report -> report.getStatus() == ReportStatus.Published)
                .filter(report -> report.getPublishedAt() != null)
                .max(Comparator.comparing(RaceReport::getPublishedAt));

        if (latestReport.isEmpty()) {
            return Optional.empty();
        }

        RaceReport report = latestReport.get();
        Race race = report.getRace();
        Tournament tournament = race.getRound().getTournament();
        List<LatestResultItem> results = raceResultRepository
                .findByRace_RaceIdOrderByRankAsc(race.getRaceId())
                .stream()
                .sorted(Comparator.comparing(
                        RaceResult::getRank,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(LATEST_RESULT_LIMIT)
                .map(this::toLatestResultItem)
                .toList();

        return Optional.of(new LatestRaceResults(
                tournament.getTournamentId(),
                tournament.getName(),
                race.getRaceId(),
                race.getName(),
                race.getStartTime(),
                race.getFinishedAt(),
                report.getPublishedAt(),
                results
        ));
    }

    private LatestResultItem toLatestResultItem(RaceResult result) {
        RaceEntry entry = result.getEntry();
        JockeyHorseContract contract = entry.getContract();
        Horse horse = contract.getHorse();
        Jockey jockey = contract.getJockey();

        return new LatestResultItem(
                result.getResultId(),
                entry.getEntryId(),
                horse.getHorseId(),
                horse.getName(),
                horse.getImageUrl(),
                jockey.getJockeyId(),
                jockey.getUser().getFullName(),
                jockey.getUser().getImageUrl(),
                result.getRank(),
                result.getFinishTime(),
                result.getStatus()
        );
    }

    private List<HorseLeaderboardItem> getHorseLeaderboard() {
        Sort sort = Sort.by(
                Sort.Order.desc("currentRating"),
                Sort.Order.desc("totalWins")
        );
        return horseRepository.findAll(PageRequest.of(0, LEADERBOARD_LIMIT, sort))
                .getContent()
                .stream()
                .map(horse -> new HorseLeaderboardItem(
                        horse.getHorseId(),
                        horse.getName(),
                        horse.getImageUrl(),
                        horse.getCurrentRating(),
                        horse.getHighestRating(),
                        horse.getTotalRaces(),
                        horse.getTotalWins(),
                        horse.getTotalTop3Finishes(),
                        horse.getWinRate()
                ))
                .toList();
    }

    private List<JockeyLeaderboardItem> getJockeyLeaderboard() {
        Sort sort = Sort.by(
                Sort.Order.desc("totalWins"),
                Sort.Order.desc("totalRaces")
        );
        return jockeyRepository.findAll(PageRequest.of(0, LEADERBOARD_LIMIT, sort))
                .getContent()
                .stream()
                .map(jockey -> new JockeyLeaderboardItem(
                        jockey.getJockeyId(),
                        jockey.getUser().getFullName(),
                        jockey.getUser().getImageUrl(),
                        jockey.getTotalRaces(),
                        jockey.getTotalWins(),
                        calculateWinRate(jockey),
                        jockey.getJockeyTier()
                ))
                .toList();
    }

    private BigDecimal calculateWinRate(Jockey jockey) {
        if (jockey.getTotalRaces() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(jockey.getTotalWins())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(jockey.getTotalRaces()), 2, RoundingMode.HALF_UP);
    }

    private Optional<FeaturedPrediction> getFeaturedPrediction(List<HomeRace> upcomingRaces) {
        for (HomeRace race : upcomingRaces) {
            List<AIPrediction> predictions = aiPredictionRepository
                    .findByEntry_Race_RaceId(race.raceId());
            if (predictions.isEmpty()) {
                continue;
            }

            List<PredictionCandidate> candidates = predictions.stream()
                    .sorted(Comparator.comparing(
                            AIPrediction::getWinProbability,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(PREDICTION_CANDIDATE_LIMIT)
                    .map(this::toPredictionCandidate)
                    .toList();
            return Optional.of(new FeaturedPrediction(race, candidates));
        }
        return Optional.empty();
    }

    private PredictionCandidate toPredictionCandidate(AIPrediction prediction) {
        RaceEntry entry = prediction.getEntry();
        Horse horse = entry.getContract().getHorse();
        Jockey jockey = entry.getContract().getJockey();

        return new PredictionCandidate(
                prediction.getPredictionId(),
                entry.getEntryId(),
                entry.getLaneNumber(),
                horse.getHorseId(),
                horse.getName(),
                horse.getImageUrl(),
                jockey.getJockeyId(),
                jockey.getUser().getFullName(),
                jockey.getUser().getImageUrl(),
                prediction.getWinProbability(),
                prediction.getConfidenceScore(),
                prediction.getPredictedTopN(),
                prediction.getPredictionReason(),
                prediction.getGeneratedAt()
        );
    }

    private HomeRace toHomeRace(RaceSummaryResponse race) {
        return new HomeRace(
                race.getTournamentId(),
                race.getTournamentName(),
                race.getRoundId(),
                race.getRoundName(),
                race.getRaceId(),
                race.getRaceName(),
                race.getStartTime(),
                race.getEndTime(),
                race.getTrackCondition(),
                race.getDistance(),
                race.getSequenceOrder(),
                race.getStatus(),
                race.getPredictionOpenAt(),
                race.getPredictionCloseAt()
        );
    }
}
