package com.swp391.horseracing.dto.race_referee.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRaceRefereeRequest {

    @NotNull(message = "Race ID is required")
    UUID raceId;

    @NotNull(message = "Referee ID is required")
    UUID refereeId;
}
