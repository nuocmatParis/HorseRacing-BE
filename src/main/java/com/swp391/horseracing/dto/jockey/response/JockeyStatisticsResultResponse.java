package com.swp391.horseracing.dto.jockey.response;

import com.swp391.horseracing.enums.RaceResultStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class JockeyStatisticsResultResponse {
    private UUID resultId;
    private UUID tournamentId;
    private String tournamentName;
    private UUID raceId;
    private String raceName;
    private UUID horseId;
    private String horseName;
    private Integer rank;
    private Float finishTime;
    private RaceResultStatus status;
    private BigDecimal jockeyPrizeAmount;
    private boolean prizePaid;
    private LocalDateTime publishedAt;
}
