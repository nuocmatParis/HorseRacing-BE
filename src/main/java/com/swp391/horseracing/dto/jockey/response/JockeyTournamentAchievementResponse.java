package com.swp391.horseracing.dto.jockey.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class JockeyTournamentAchievementResponse {
    private UUID tournamentId;
    private String tournamentName;
    private int totalRaces;
    private int totalWins;
    private int totalTop3Finishes;
    private BigDecimal prizeReceived;
}
