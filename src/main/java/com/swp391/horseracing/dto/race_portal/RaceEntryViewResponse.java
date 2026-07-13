package com.swp391.horseracing.dto.race_portal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swp391.horseracing.enums.RaceEntryStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaceEntryViewResponse {
    private UUID entryId;
    private int laneNumber;
    private RaceEntryStatus status;
    private UUID horseId;
    private String horseName;
    private UUID jockeyId;
    private String jockeyName;
    private String scratchedReason;
    private String disqualifiedReason;
}
