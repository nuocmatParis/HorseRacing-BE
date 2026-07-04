package com.swp391.horseracing.dto.race_entry.request;

import com.swp391.horseracing.enums.RaceEntryStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRaceEntryRequest {

    RaceEntryStatus status;

    String withdrawReason;

    String scratchedReason;
}
