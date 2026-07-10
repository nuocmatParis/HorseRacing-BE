package com.swp391.horseracing.dto.jockeyinspection.request;

import com.swp391.horseracing.enums.InspectionResult;
import jakarta.validation.constraints.NotNull;
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
}
