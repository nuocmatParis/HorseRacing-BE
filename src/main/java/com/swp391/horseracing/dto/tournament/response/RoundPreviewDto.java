package com.swp391.horseracing.dto.tournament.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoundPreviewDto {
    int sequenceOrder;
    int raceCount;
    List<Integer> entriesPerRace;
    boolean isFinal;
}
