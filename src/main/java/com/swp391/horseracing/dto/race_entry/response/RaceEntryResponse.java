package com.swp391.horseracing.dto.race_entry.response;

import com.swp391.horseracing.enums.RaceEntryStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceEntryResponse {

    UUID entryId;
    UUID raceId;
    UUID contractId;
    Integer laneNumber;
    RaceEntryStatus status;
    UUID assignedById;
    LocalDateTime assignedAt;
    LocalDateTime withdrawnAt;
    String withdrawReason;
    String scratchedReason;
    LocalDateTime disqualifiedAt;
    LocalDateTime createdAt;
}
