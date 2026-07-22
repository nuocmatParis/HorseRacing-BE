package com.swp391.horseracing.dto.bracket;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoundPlan {
    int sequenceOrder;
    String roundName;
    int raceCount;
    int entriesPerRace;
    int qualifiersPerRace;
    boolean isFinal;
    LocalDateTime estimatedStartDate;
    LocalDateTime estimatedEndDate;
    List<RacePlan> races;
}
