package com.swp391.horseracing.dto.jockey.response;

import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.RaceClass;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JockeyAssignedHorseResponse {
    private UUID horseId;
    private String horseName;
    private String imageUrl;
    private UUID ownerId;
    private String ownerName;
    private UUID tournamentId;
    private String tournamentName;
    private UUID contractId;
    private ContractStatus contractStatus;
    private int currentRating;
    private RaceClass raceClass;
    private HealthStatus healthStatus;
    private int totalRaces;
    private int totalWins;
    private int totalTop3Finishes;
    private Double winRate;
    private UUID nextRaceId;
    private String nextRaceName;
    private LocalDateTime nextRaceStartTime;
    private Integer laneNumber;
}
