package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TournamentRatingConfigRequest {

    @Min(value = 0, message = "First place minimum rating change must be non-negative")
    Integer firstMin;

    @Min(value = 0, message = "First place maximum rating change must be non-negative")
    Integer firstMax;

    @Min(value = 0, message = "Second place minimum rating change must be non-negative")
    Integer secondMin;

    @Min(value = 0, message = "Second place maximum rating change must be non-negative")
    Integer secondMax;

    @Min(value = 0, message = "Third place minimum rating change must be non-negative")
    Integer thirdMin;

    @Min(value = 0, message = "Third place maximum rating change must be non-negative")
    Integer thirdMax;

    @Min(value = 0, message = "Fourth and fifth place minimum rating change must be non-negative")
    Integer fourthFifthMin;

    @Min(value = 0, message = "Fourth and fifth place maximum rating change must be non-negative")
    Integer fourthFifthMax;

    @Max(value = 0, message = "Other finishers minimum rating change must not be positive")
    Integer otherMin;

    @Max(value = 0, message = "Other finishers maximum rating change must not be positive")
    Integer otherMax;

    @Max(value = 0, message = "Disqualified minimum rating change must not be positive")
    Integer disqualifiedMin;

    @Max(value = 0, message = "Disqualified maximum rating change must not be positive")
    Integer disqualifiedMax;
}
