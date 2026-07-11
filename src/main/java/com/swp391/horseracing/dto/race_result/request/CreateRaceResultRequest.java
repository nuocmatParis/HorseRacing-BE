package com.swp391.horseracing.dto.race_result.request;

import com.swp391.horseracing.enums.RaceResultStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRaceResultRequest {

    @NotNull(message = "Race ID is required")
    UUID raceId;

    @NotNull(message = "Entry ID is required")
    UUID entryId;

    @NotNull(message = "Finish time is required")
    @PositiveOrZero(message = "Finish time must be zero or positive")
    Float finishTime;

    @NotNull(message = "Rank is required")
    @Min(value = 1, message = "Rank must be at least 1")
    Integer rank;

    @NotNull(message = "Result status is required")
    RaceResultStatus status;
}
