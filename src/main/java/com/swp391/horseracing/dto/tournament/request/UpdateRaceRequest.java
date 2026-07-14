package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.RaceDistance;
import com.swp391.horseracing.enums.RoundStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRaceRequest {

    @Size(max = 150, message = "Race name must not exceed 150 characters")
    String name;

    LocalDateTime startTime;

    @Size(max = 100, message = "Track condition must not exceed 100 characters")
    String trackCondition;

    RaceDistance distance;

    @Min(value = 1, message = "Sequence order must be at least 1")
    Integer sequenceOrder;
}
