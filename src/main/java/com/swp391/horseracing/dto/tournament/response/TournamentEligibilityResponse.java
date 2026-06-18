package com.swp391.horseracing.dto.tournament.response;

import com.swp391.horseracing.enums.EligibilityTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TournamentEligibilityResponse {

    UUID eligibilityId;
    EligibilityTargetType targetType;
    String conditionName;
    String conditionOperator;
    String conditionValue;
    Boolean isActive;
    UUID tournamentId;
}
