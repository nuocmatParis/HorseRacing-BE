package com.swp391.horseracing.dto.race_entry.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRaceEntryRequest {

    @NotNull(message = "Race ID is required")
    UUID raceId;

    @NotNull(message = "Contract ID is required")
    UUID contractId;

    @Min(value = 1, message = "Lane number must be at least 1")
    Integer laneNumber;
}
