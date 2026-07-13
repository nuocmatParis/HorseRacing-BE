package com.swp391.horseracing.dto.race_portal;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RaceResultsResponse {
    private RaceSummaryResponse race;
    private UUID reportId;
    private LocalDateTime publishedAt;
    private List<RaceResultItemResponse> myResults;
}
