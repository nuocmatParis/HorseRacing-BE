package com.swp391.horseracing.dto.tournament.response;

import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityOperator;
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
    EligibilityCondition conditionName;
    EligibilityOperator conditionOperator;
    String conditionValue;
    String unit;
    Boolean isActive;
    UUID tournamentId;
}
