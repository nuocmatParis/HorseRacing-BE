package com.swp391.horseracing.dto.prediction.response;

import com.swp391.horseracing.enums.PredictionDetailStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PredictionDetailResponse {

    UUID predictionDetailId;
    UUID entryId;
    int predictedRank;
    PredictionDetailStatus status;
    Integer awardedPoints;
}
