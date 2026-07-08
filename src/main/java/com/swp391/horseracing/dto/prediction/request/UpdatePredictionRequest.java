package com.swp391.horseracing.dto.prediction.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdatePredictionRequest {

    @NotEmpty(message = "At least one prediction entry is required")
    List<PredictionEntryRequest> entries;
}
