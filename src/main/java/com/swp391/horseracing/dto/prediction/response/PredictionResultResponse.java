package com.swp391.horseracing.dto.prediction.response;

import com.swp391.horseracing.enums.PredictionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PredictionResultResponse {
    private UUID predictionId;
    private UUID raceId;
    private PredictionStatus status;
    private Integer rewardPoints;
    private LocalDateTime scoredAt;
    private LocalDateTime voidedAt;
    private String voidReason;
    private List<PredictionResultDetailResponse> predictions;
    private List<OfficialRaceResultResponse> officialTopThree;
}
