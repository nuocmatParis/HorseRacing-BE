package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityOperator;
import com.swp391.horseracing.enums.EligibilityTargetType;
import com.swp391.horseracing.validation.ValidEligibilityTarget;
import com.swp391.horseracing.validation.ValidEligibilityValue;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ValidEligibilityValue
@ValidEligibilityTarget
public class UpdateEligibilityRequest {

    EligibilityTargetType targetType;

    EligibilityCondition conditionName;

    EligibilityOperator conditionOperator;

    String conditionValue;

    Boolean isActive;
}
