package com.swp391.horseracing.dto.prediction.request;

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
public class PredictionEntryRequest {

    @NotNull(message = "Entry ID is required")
    UUID entryId;

    @Min(value = 1, message = "Predicted rank must be at least 1")
    int predictedRank;
}
