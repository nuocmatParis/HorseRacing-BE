package com.swp391.horseracing.dto.prediction.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIPredictionResponse {

    UUID predictionId;
    UUID entryId;
    int laneNumber;
    String horseName;
    String jockeyName;
    int horseRating;
    int predictedTopN;
    BigDecimal topNProbability;
    BigDecimal winProbability;
    BigDecimal confidenceScore;
    String predictionReason;
    String modelVersion;
    LocalDateTime generatedAt;
}
