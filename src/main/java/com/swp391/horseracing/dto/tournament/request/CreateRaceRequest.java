package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.RoundStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRaceRequest {

    @NotBlank(message = "Race name is required")
    @Size(max = 150, message = "Race name must not exceed 150 characters")
    String name;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be now or in the future")
    LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @FutureOrPresent(message = "End time must be now or in the future")
    LocalDateTime endTime;

    @NotNull(message = "Track condition is required")
    @Size(max = 100, message = "Track condition must not exceed 100 characters")
    String trackCondition;

    @NotNull(message = "Distance is required")
    @Positive(message = "Distance must be positive")
    Float distance;

    @NotNull(message = "Max entries is required")
    @Min(value = 1, message = "Max entries must be at least 1")
    Integer maxEntries;

    @NotNull(message = "Status is required")
    RoundStatus status;

    @NotNull(message = "Schedule published time is required")
    LocalDateTime schedulePublishedAt;

    @NotNull(message = "Prediction open time is required")
    LocalDateTime predictionOpenAt;

    @NotNull(message = "Prediction close time is required")
    LocalDateTime predictionCloseAt;
}
