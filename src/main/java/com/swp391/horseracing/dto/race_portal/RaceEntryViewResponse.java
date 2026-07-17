package com.swp391.horseracing.dto.race_portal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.InspectionResult;
import com.swp391.horseracing.enums.InspectionStatus;
import com.swp391.horseracing.enums.HorseBreed;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaceEntryViewResponse {
    private UUID entryId;
    private Integer laneNumber;
    private RaceEntryStatus status;
    private UUID horseId;
    private String horseName;
    private HorseBreed horseBreed;
    private Float horseRegisteredWeight;
    private UUID jockeyId;
    private String jockeyName;
    private Float jockeyRegisteredWeight;
    private String scratchedReason;
    private String disqualifiedReason;
    private UUID horseInspectionId;
    private InspectionStatus horseInspectionStatus;
    private InspectionResult horseInspectionResult;
    private UUID jockeyInspectionId;
    private InspectionStatus jockeyInspectionStatus;
    private InspectionResult jockeyInspectionResult;
    private BigDecimal winProbability;
    private BigDecimal topNProbability;
    private BigDecimal confidenceScore;
    private String predictionReason;
}
