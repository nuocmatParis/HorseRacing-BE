package com.swp391.horseracing.dto.jockeyinspection.request;

import com.swp391.horseracing.enums.InspectionResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JockeyInspectionRequest {
    @NotNull(message = "Inspection result is required")
    InspectionResult result;

    String note;

    @NotNull(message = "Actual jockey weight is required")
    @Positive(message = "Actual jockey weight must be greater than 0")
    Float actualWeight;

    @NotNull(message = "Doping result is required")
    Boolean dopingDetected;
}
