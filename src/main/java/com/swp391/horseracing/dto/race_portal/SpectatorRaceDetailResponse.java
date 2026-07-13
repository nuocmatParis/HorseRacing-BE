package com.swp391.horseracing.dto.race_portal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpectatorRaceDetailResponse {
    private RaceSummaryResponse race;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime rescheduledAt;
    private String rescheduleReason;
    private List<RaceEntryViewResponse> entries;
}
