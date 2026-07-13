package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RoundStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoundRequest {

    @Size(max = 100, message = "Round name must not exceed 100 characters")
    String roundName;

    @Min(value = 1, message = "Sequence order must be at least 1")
    Integer sequenceOrder;

    Boolean isFinal;

    PredictionType predictionType;

    String advancementRule;

    LocalDateTime startDate;

    LocalDateTime endDate;

    String description;

    @Min(value = 1, message = "Max races must be at least 1")
    Integer maxRaces;

    @Min(value = 1, message = "Max entries must be at least 1")
    Integer maxEntries;

    @Min(value = 1, message = "Min entries must be at least 1")
    Integer minEntries;

    RoundStatus status;

    UUID headRefereeId;
}
