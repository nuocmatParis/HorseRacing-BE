package com.swp391.horseracing.dto.tournament.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BracketPreviewResponse {
    Integer maxApprovedEntries;
    Integer actualApprovedEntries;
    Integer minEntriesPerRace;
    Integer maxEntriesPerRace;
    Integer qualifiersPerRace;
    Integer predictionPositions;
    Integer finalPrizePositions;
    List<RoundPreviewDto> rounds;
    Integer totalRaceCount;
    boolean valid;
    String errorMessage;
    Integer recommendedMaxApprovedEntries;
    Integer minimumRacingDays;
    Integer minimumTournamentCalendarDays;
    LocalDate earliestPossibleEndDate;
    boolean scheduleFeasible;
}
