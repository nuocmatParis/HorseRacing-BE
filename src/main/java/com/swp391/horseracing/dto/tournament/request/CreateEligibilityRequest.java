package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityOperator;
import com.swp391.horseracing.enums.EligibilityTargetType;
import com.swp391.horseracing.validation.ValidEligibilityTarget;
import com.swp391.horseracing.validation.ValidEligibilityValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ValidEligibilityValue
@ValidEligibilityTarget
public class CreateEligibilityRequest {

    @NotNull(message = "Target type is required")
    EligibilityTargetType targetType;

    @NotNull(message = "Condition name is required")
    EligibilityCondition conditionName;

    @NotNull(message = "Condition operator is required")
    EligibilityOperator conditionOperator;

    @NotBlank(message = "Condition value is required")
    String conditionValue;

    @NotNull(message = "Active status is required")
    Boolean isActive;
}
