package com.swp391.horseracing.dto.prediction.response;

import com.swp391.horseracing.enums.TrackCondition;
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
    BigDecimal horseCurrentRating;
    BigDecimal horseRecentForm;
    BigDecimal horseWinRate;
    BigDecimal horseTop3Rate;
    BigDecimal jockeyWinRate;
    BigDecimal jockeyTop3Rate;
    BigDecimal jockeyRecentForm;
    BigDecimal pairWinRate;
    BigDecimal pairTop3Rate;
    int raceDistance;
    TrackCondition trackCondition;
    int numberOfCompetitors;
    int laneNumber;
    BigDecimal assignedWeightKg;
    BigDecimal actualCarriedWeightKg;
    BigDecimal carriedWeightRatio;
    BigDecimal relativeRating;
    BigDecimal winProbability;
    int predictedRank;
    BigDecimal confidenceScore;
    String modelVersion;
    LocalDateTime generatedAt;
    LocalDateTime createdAt;
}
