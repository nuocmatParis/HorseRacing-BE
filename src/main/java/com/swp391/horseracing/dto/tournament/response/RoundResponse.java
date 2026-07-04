package com.swp391.horseracing.dto.tournament.response;

import com.swp391.horseracing.enums.PredictionType;
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
public class RoundResponse {

    UUID roundId;
    String roundName;
    int sequenceOrder;
    boolean isFinal;
    PredictionType predictionType;
    String advancementRule;
    LocalDateTime startDate;
    LocalDateTime endDate;
    String description;
    Integer maxRaces;
    RoundStatus status;
    LocalDateTime createdAt;
    UUID tournamentId;
    UUID createdById;
    UUID headRefereeId;
    LocalDateTime headRefereeAssignedAt;
}
