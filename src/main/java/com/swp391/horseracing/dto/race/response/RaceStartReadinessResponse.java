package com.swp391.horseracing.dto.race.response;

import com.swp391.horseracing.enums.RoundStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RaceStartReadinessResponse {
    private UUID raceId;
    private RoundStatus raceStatus;
    private boolean canStart;
    private LocalDateTime inspectionFinalizedAt;
    private RaceStartWindowResponse startWindow;
    private int activeEntryCount;
    private int minEntries;
    private List<String> blockingReasons;
    private List<RaceEntryReadinessResponse> entries;
}
