package com.swp391.horseracing.dto.appeal.response;

import com.swp391.horseracing.enums.AppealStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealResponse {

    UUID appealId;
    UUID entryId;
    UUID raceId;
    String raceName;
    String roundName;
    String tournamentName;
    String horseName;
    String jockeyName;
    UUID raceResultId;
    UUID relatedViolationId;
    UUID categoryId;
    String categoryName;
    UUID submittedByUserId;
    String description;
    AppealStatus status;
    LocalDateTime submittedAt;
    UUID reviewedByRefereeId;
    LocalDateTime reviewedAt;
    String resolution;
}
