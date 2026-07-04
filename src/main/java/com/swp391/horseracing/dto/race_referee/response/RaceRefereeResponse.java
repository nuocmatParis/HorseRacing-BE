package com.swp391.horseracing.dto.race_referee.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceRefereeResponse {

    UUID raceRefereeId;
    UUID raceId;
    UUID refereeId;
    UUID assignedById;
    LocalDateTime assignedAt;
}
