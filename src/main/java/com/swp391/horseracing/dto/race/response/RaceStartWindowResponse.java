package com.swp391.horseracing.dto.race.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RaceStartWindowResponse {
    private LocalDateTime earliestStart;
    private LocalDateTime latestStart;
    private LocalDateTime serverNow;
}
