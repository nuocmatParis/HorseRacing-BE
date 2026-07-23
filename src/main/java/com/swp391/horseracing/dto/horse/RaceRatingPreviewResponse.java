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
public class RaceRatingPreviewResponse {
    private UUID raceId;
    private String reportStatus;
    private int policyVersion;
    private List<HorseRatingPreviewItem> changes;
}
