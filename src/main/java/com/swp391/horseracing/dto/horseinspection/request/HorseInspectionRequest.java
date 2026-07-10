package com.swp391.horseracing.dto.horseinspection.request;

import com.swp391.horseracing.enums.InspectionResult;
import jakarta.validation.constraints.NotNull;
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
}
