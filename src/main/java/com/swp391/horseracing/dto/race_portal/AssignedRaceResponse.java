package com.swp391.horseracing.dto.race_portal;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AssignedRaceResponse {
    private RaceSummaryResponse race;
    private String assignmentRole;
    private LocalDateTime assignedAt;
    private LocalDateTime inspectionOpenAt;
    private LocalDateTime inspectionCloseAt;
    private TournamentInspectionConditionsResponse tournamentConditions;
    private int entryCount;
    private List<RaceEntryViewResponse> entries;
}
