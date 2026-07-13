package com.swp391.horseracing.dto.race_portal;

import com.swp391.horseracing.enums.RoundStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RaceSummaryResponse {
    private UUID tournamentId;
    private String tournamentName;
    private UUID roundId;
    private String roundName;
    private UUID raceId;
    private String raceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String trackCondition;
    private Float distance;
    private int sequenceOrder;
    private RoundStatus status;
    private LocalDateTime predictionOpenAt;
    private LocalDateTime predictionCloseAt;
    private LocalDateTime schedulePublishedAt;
}
