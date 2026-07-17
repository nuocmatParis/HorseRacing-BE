package com.swp391.horseracing.dto.race_entry.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCreateRaceEntryRequest {

    @NotNull(message = "Contract ID is required")
    UUID contractId;

    @Min(value = 1, message = "Lane number must be at least 1")
    Integer laneNumber;
}
