package com.swp391.horseracing.dto.violation.request;
import com.swp391.horseracing.enums.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViolationCreateRequest {
    @NotNull(message = "Violation type is required")
    ViolationType type;

    String description;

    @NotNull(message = "Penalty type is required")
    PenaltyType penaltyType;

    Float penaltyValue;

    LocalDateTime occurredAt;
}
