package com.swp391.horseracing.dto.race.response;

import com.swp391.horseracing.enums.InspectionResult;
import com.swp391.horseracing.enums.InspectionStatus;
import com.swp391.horseracing.enums.RaceEntryStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RaceEntryReadinessResponse {
    private UUID entryId;
    private String horseName;
    private String jockeyName;
    private RaceEntryStatus entryStatus;
    private InspectionStatus horseInspectionStatus;
    private InspectionResult horseInspectionResult;
    private InspectionStatus jockeyInspectionStatus;
    private InspectionResult jockeyInspectionResult;
    private boolean canRace;
    private List<String> blockingReasons;
}
