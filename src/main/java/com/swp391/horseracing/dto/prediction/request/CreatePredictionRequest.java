package com.swp391.horseracing.dto.prediction.request;

import com.swp391.horseracing.enums.PredictionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePredictionRequest {

    @NotNull(message = "Prediction type is required")
    PredictionType predictionType;

    @NotEmpty(message = "At least one prediction entry is required")
    List<PredictionEntryRequest> entries;
}
