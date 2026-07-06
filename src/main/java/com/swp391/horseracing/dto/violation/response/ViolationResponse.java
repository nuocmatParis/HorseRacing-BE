package com.swp391.horseracing.dto.violation.response;

import com.swp391.horseracing.enums.PenaltyType;
import com.swp391.horseracing.enums.ViolationStatus;
import com.swp391.horseracing.enums.ViolationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViolationResponse {

    UUID violationId;
    UUID entryId;
    UUID refereeId;
    ViolationType type;
    String description;
    PenaltyType penaltyType;
    Float penaltyValue;
    LocalDateTime occurredAt;
    LocalDateTime createdAt;
    ViolationStatus status;
}
