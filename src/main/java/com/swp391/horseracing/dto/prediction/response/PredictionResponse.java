package com.swp391.horseracing.dto.prediction.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swp391.horseracing.enums.PredictionStatus;
import com.swp391.horseracing.enums.PredictionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PredictionResponse {

    UUID predictionId;
    UUID spectatorId;
    UUID raceId;
    PredictionType predictionType;
    LocalDateTime predictionTime;
    PredictionStatus status;
    Integer rewardPoints;
    LocalDateTime scoredAt;
    List<PredictionDetailResponse> details;
    List<AIPredictionResponse> aiPredictions;
}
