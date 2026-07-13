package com.swp391.horseracing.dto.prediction.response;

import com.swp391.horseracing.enums.PredictionDetailStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PredictionResultDetailResponse {
    private UUID entryId;
    private UUID horseId;
    private String horseName;
    private int predictedRank;
    private Integer officialRank;
    private PredictionDetailStatus status;
    private Integer awardedPoints;
}
