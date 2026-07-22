package com.swp391.horseracing.dto.bracket;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RacePlan {
    int sequenceOrder;
    String name;
    LocalDateTime startTime;
    LocalDateTime endTime;
}
