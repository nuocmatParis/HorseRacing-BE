package com.swp391.horseracing.dto.bracket;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BracketStructure {
    int totalEntries;
    int roundCount;
    List<RoundPlan> rounds;
}
