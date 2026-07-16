package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.jockey.response.JockeyStatisticsResponse;
import com.swp391.horseracing.dto.jockey.response.JockeyStatisticsResultResponse;
import com.swp391.horseracing.dto.jockey.response.JockeyTournamentAchievementResponse;
import com.swp391.horseracing.entity.RaceReport;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.RaceResultStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.JockeyRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.service.JockeyStatisticsService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JockeyStatisticsServiceImpl implements JockeyStatisticsService {
    UserCurrentService userCurrentService;
    JockeyRepository jockeyRepository;
    RaceResultRepository raceResultRepository;
    RaceReportRepository raceReportRepository;

    @Override
    @Transactional(readOnly = true)
    public JockeyStatisticsResponse getMyStatistics() {
        User user = userCurrentService.getCurrentUser();
        if (!jockeyRepository.existsByUser_UserId(user.getUserId())) {
            throw new AppException(ErrorCode.JOCKEY_PROFILE_NOT_FOUND);
        }
        List<RaceResult> source = raceResultRepository.findAllForJockeyStatistics(user.getUserId());
        List<RaceResult> officialResults = new ArrayList<>();
        for (RaceResult result : source) {
            Optional<RaceReport> report = raceReportRepository.findByRace_RaceId(result.getRace().getRaceId());
            if (report.isPresent() && report.get().getStatus() == ReportStatus.PUBLISHED) {
                officialResults.add(result);
            }
        }

        int first = 0;
        int second = 0;
        int third = 0;
        BigDecimal totalPrize = BigDecimal.ZERO;
        Map<UUID, AchievementAccumulator> byTournament = new LinkedHashMap<>();
        List<JockeyStatisticsResultResponse> recentResults = new ArrayList<>();

        for (RaceResult result : officialResults) {
            Integer rank = result.getRank();
            if (result.getStatus() == RaceResultStatus.FINISHED && rank != null) {
                if (rank == 1) first++;
                if (rank == 2) second++;
                if (rank == 3) third++;
            }
            if (result.isPrizePaid() && result.getJockeyPrizeAmount() != null) {
                totalPrize = totalPrize.add(result.getJockeyPrizeAmount());
            }

            Tournament tournament = result.getRace().getRound().getTournament();
            AchievementAccumulator accumulator = byTournament.get(tournament.getTournamentId());
            if (accumulator == null) {
                accumulator = new AchievementAccumulator(tournament.getTournamentId(), tournament.getName());
                byTournament.put(tournament.getTournamentId(), accumulator);
            }
            accumulator.totalRaces++;
            if (rank != null && rank == 1 && result.getStatus() == RaceResultStatus.FINISHED) {
                accumulator.totalWins++;
            }
            if (rank != null && rank >= 1 && rank <= 3 && result.getStatus() == RaceResultStatus.FINISHED) {
                accumulator.totalTop3++;
            }
            if (result.isPrizePaid() && result.getJockeyPrizeAmount() != null) {
                accumulator.prize = accumulator.prize.add(result.getJockeyPrizeAmount());
            }

            if (recentResults.size() < 20) {
                RaceReport report = raceReportRepository.findByRace_RaceId(result.getRace().getRaceId()).orElse(null);
                recentResults.add(JockeyStatisticsResultResponse.builder()
                        .resultId(result.getResultId())
                        .tournamentId(tournament.getTournamentId())
                        .tournamentName(tournament.getName())
                        .raceId(result.getRace().getRaceId())
                        .raceName(result.getRace().getName())
                        .horseId(result.getEntry().getContract().getHorse().getHorseId())
                        .horseName(result.getEntry().getContract().getHorse().getName())
                        .rank(rank)
                        .finishTime(result.getFinishTime())
                        .status(result.getStatus())
                        .jockeyPrizeAmount(result.getJockeyPrizeAmount())
                        .prizePaid(result.isPrizePaid())
                        .publishedAt(report == null ? null : report.getPublishedAt())
                        .build());
            }
        }

        int totalRaces = officialResults.size();
        int top3 = first + second + third;
        List<JockeyTournamentAchievementResponse> tournamentItems = new ArrayList<>();
        for (AchievementAccumulator value : byTournament.values()) {
            tournamentItems.add(JockeyTournamentAchievementResponse.builder()
                    .tournamentId(value.tournamentId)
                    .tournamentName(value.tournamentName)
                    .totalRaces(value.totalRaces)
                    .totalWins(value.totalWins)
                    .totalTop3Finishes(value.totalTop3)
                    .prizeReceived(value.prize)
                    .build());
        }

        return JockeyStatisticsResponse.builder()
                .totalRaces(totalRaces)
                .totalWins(first)
                .winRate(rate(first, totalRaces))
                .firstPlaceCount(first)
                .secondPlaceCount(second)
                .thirdPlaceCount(third)
                .totalTop3Finishes(top3)
                .top3Rate(rate(top3, totalRaces))
                .totalPrizeReceived(totalPrize)
                .byTournament(tournamentItems)
                .recentResults(recentResults)
                .build();
    }

    private double rate(int count, int total) {
        if (total == 0) return 0D;
        return BigDecimal.valueOf(count * 100D / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static final class AchievementAccumulator {
        private final UUID tournamentId;
        private final String tournamentName;
        private int totalRaces;
        private int totalWins;
        private int totalTop3;
        private BigDecimal prize = BigDecimal.ZERO;

        private AchievementAccumulator(UUID tournamentId, String tournamentName) {
            this.tournamentId = tournamentId;
            this.tournamentName = tournamentName;
        }
    }
}
