package com.swp391.horseracing.dto.violation.response;

import com.swp391.horseracing.enums.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViolationResponse {
    UUID violationId;

    UUID raceId;
    UUID entryId;

    UUID horseId;
    String horseName;

    UUID jockeyId;
    String jockeyName;

    UUID refereeId;
    String refereeName;

    ViolationType type;

    String description;

    PenaltyType penaltyType;

    Float penaltyValue;

    LocalDateTime occurredAt;
    LocalDateTime createdAt;
    ViolationStatus status;
}
