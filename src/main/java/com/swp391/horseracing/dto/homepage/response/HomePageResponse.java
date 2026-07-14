package com.swp391.horseracing.dto.homepage.response;

import com.swp391.horseracing.enums.JockeyTier;
import com.swp391.horseracing.enums.RaceDistance;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HomePageResponse(
        HomeStats stats,
        FeaturedTournament featuredTournament,
        List<HomeRace> upcomingRaces,
        LatestRaceResults latestResults,
        List<HorseLeaderboardItem> horseLeaderboard,
        List<JockeyLeaderboardItem> jockeyLeaderboard,
        FeaturedPrediction featuredPrediction,
        List<HomeRace> todaySchedule,
        LocalDateTime generatedAt
) {
    public record HomeStats(
            long tournamentCount,
            long horseCount,
            long jockeyCount,
            long raceCount
    ) {
    }

    public record FeaturedTournament(
            UUID tournamentId,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            BigDecimal totalPrizePool,
            TournamentStatus status,
            TournamentPhase phase,
            String currentRoundName
    ) {
    }

    public record HomeRace(
            UUID tournamentId,
            String tournamentName,
            UUID roundId,
            String roundName,
            UUID raceId,
            String raceName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String trackCondition,
            RaceDistance distance,
            int sequenceOrder,
            RoundStatus status,
            LocalDateTime predictionOpenAt,
            LocalDateTime predictionCloseAt
    ) {
    }

    public record LatestRaceResults(
            UUID tournamentId,
            String tournamentName,
            UUID raceId,
            String raceName,
            LocalDateTime startTime,
            LocalDateTime finishedAt,
            LocalDateTime publishedAt,
            List<LatestResultItem> results
    ) {
    }

    public record LatestResultItem(
            UUID resultId,
            UUID entryId,
            UUID horseId,
            String horseName,
            String horseImageUrl,
            UUID jockeyId,
            String jockeyName,
            String jockeyImageUrl,
            Integer rank,
            Float finishTime,
            RaceResultStatus status
    ) {
    }

    public record HorseLeaderboardItem(
            UUID horseId,
            String horseName,
            String imageUrl,
            int currentRating,
            int highestRating,
            int totalRaces,
            int totalWins,
            int totalTop3Finishes,
            Double winRate
    ) {
    }

    public record JockeyLeaderboardItem(
            UUID jockeyId,
            String jockeyName,
            String imageUrl,
            int totalRaces,
            int totalWins,
            BigDecimal winRate,
            JockeyTier jockeyTier
    ) {
    }

    public record FeaturedPrediction(
            HomeRace race,
            List<PredictionCandidate> candidates
    ) {
    }

    public record PredictionCandidate(
            UUID predictionId,
            UUID entryId,
            Integer laneNumber,
            UUID horseId,
            String horseName,
            String horseImageUrl,
            UUID jockeyId,
            String jockeyName,
            String jockeyImageUrl,
            BigDecimal winProbability,
            BigDecimal confidenceScore,
            int predictedTopN,
            String predictionReason,
            LocalDateTime generatedAt
    ) {
    }
}
