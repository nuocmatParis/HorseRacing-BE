package com.swp391.horseracing.dto.horse;

import com.swp391.horseracing.enums.RaceClass;
import com.swp391.horseracing.enums.RaceResultStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorseRatingPreviewItem {
    private UUID horseId;
    private String horseName;
    private Integer finishPosition;
    private RaceResultStatus resultStatus;
    private int oldRating;
    private int minimumAllowedChange;
    private int maximumAllowedChange;
    private int finalChange;
    private String adjustmentReason;
    private int newRating;
    private RaceClass oldRaceClass;
    private RaceClass newRaceClass;
}
