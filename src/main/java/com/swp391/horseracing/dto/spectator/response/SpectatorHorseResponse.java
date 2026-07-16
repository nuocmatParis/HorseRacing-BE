package com.swp391.horseracing.dto.spectator.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.RaceClass;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpectatorHorseResponse {
    private UUID horseId;
    private String horseName;
    private String imageUrl;
    private HorseBreed breed;
    private int age;
    private float weight;
    private RaceClass raceClass;
    private int currentRating;
    private int totalRaces;
    private int totalWins;
    private int totalTop3Finishes;
    private Double winRate;
    private HealthStatus healthStatus;
    private UUID ownerId;
    private String ownerName;
    private boolean followedByCurrentUser;
    private LocalDateTime followedAt;
    private UUID nextRaceId;
    private String nextRaceName;
    private String nextTournamentName;
    private LocalDateTime nextRaceStartTime;
}
