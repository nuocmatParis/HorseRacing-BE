package com.swp391.horseracing.dto.tournament.response;

import com.swp391.horseracing.enums.RaceDistance;
import com.swp391.horseracing.enums.RoundStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceResponse {

    UUID raceId;
    String name;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String trackCondition;
    RaceDistance distance;
    int sequenceOrder;
    RoundStatus status;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    LocalDateTime schedulePublishedAt;
    LocalDateTime predictionOpenAt;
    LocalDateTime predictionCloseAt;
    UUID roundId;
    UUID createdById;
}
