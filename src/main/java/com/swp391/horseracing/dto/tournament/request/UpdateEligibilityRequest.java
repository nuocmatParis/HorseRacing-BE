package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.EligibilityTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEligibilityRequest {

    EligibilityTargetType targetType;

    String conditionName;

    String conditionOperator;

    String conditionValue;

    Boolean isActive;
}
