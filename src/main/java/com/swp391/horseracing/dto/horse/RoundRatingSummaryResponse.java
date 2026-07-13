package com.swp391.horseracing.dto.horse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundRatingSummaryResponse {
    private UUID roundId;
    private String summaryStatus;
    private int publishedRaces;
    private int totalRaces;
    private List<RaceRatingChangesResponse> races;
}
