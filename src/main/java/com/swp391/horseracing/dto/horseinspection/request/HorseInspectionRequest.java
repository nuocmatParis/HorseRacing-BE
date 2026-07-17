package com.swp391.horseracing.dto.horseinspection.request;

import com.swp391.horseracing.enums.InspectionResult;
import com.swp391.horseracing.enums.HorseBreed;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HorseInspectionRequest {

    @NotNull(message = "Inspection result is required")
    InspectionResult result;

    String note;

    @PositiveOrZero(message = "Handicap weight must be greater than or equal to 0")
    Float handicapWeight;

    Boolean handicapConfirmed;

    @NotNull(message = "Actual horse weight is required")
    @Positive(message = "Actual horse weight must be greater than 0")
    Float actualWeight;

    @NotNull(message = "Actual horse breed is required")
    HorseBreed actualBreed;

    @NotNull(message = "Doping result is required")
    Boolean dopingDetected;
}
