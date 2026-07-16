package com.swp391.horseracing.dto.jockey.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class JockeyStatisticsResponse {
    private int totalRaces;
    private int totalWins;
    private double winRate;
    private int firstPlaceCount;
    private int secondPlaceCount;
    private int thirdPlaceCount;
    private int totalTop3Finishes;
    private double top3Rate;
    private BigDecimal totalPrizeReceived;
    private List<JockeyTournamentAchievementResponse> byTournament;
    private List<JockeyStatisticsResultResponse> recentResults;
}
