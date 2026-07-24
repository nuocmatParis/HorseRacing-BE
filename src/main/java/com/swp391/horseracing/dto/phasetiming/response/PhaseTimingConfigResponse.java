package com.swp391.horseracing.dto.phasetiming.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhaseTimingConfigResponse {
    Long id;
    String phaseName;
    int minCapacity;
    int maxCapacity;
    int durationDays;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
