package com.swp391.horseracing.dto.horse;

import com.swp391.horseracing.enums.RaceClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorseRatingCalculation {
    private UUID horseId;
    private String horseName;
    private Integer finishPosition;
    private int oldRating;
    private int baseChange;
    private int opponentStrengthBonus;
    private int finishPerformanceBonus;
    private int fieldSizeBonus;
    private int underperformancePenalty;
    private int finalChange;
    private int newRating;
    private RaceClass oldRaceClass;
    private RaceClass newRaceClass;
}
