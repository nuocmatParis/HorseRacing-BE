package com.swp391.horseracing.dto.race_portal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RaceScheduleResponse {
    private RaceSummaryResponse race;
    private List<RaceEntryViewResponse> myEntries;
}
