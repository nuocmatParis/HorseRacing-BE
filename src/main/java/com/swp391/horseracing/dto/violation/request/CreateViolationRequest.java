package com.swp391.horseracing.dto.violation.request;

import com.swp391.horseracing.enums.PenaltyType;
import com.swp391.horseracing.enums.ViolationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateViolationRequest {

    @NotNull(message = "Entry ID is required")
    UUID entryId;

    @NotNull(message = "Referee ID is required")
    UUID refereeId;

    @NotNull(message = "Violation type is required")
    ViolationType type;

    @NotBlank(message = "Description is required")
    String description;

    @NotNull(message = "Penalty type is required")
    PenaltyType penaltyType;

    Float penaltyValue;

    @NotNull(message = "Occurred at is required")
    LocalDateTime occurredAt;
}
