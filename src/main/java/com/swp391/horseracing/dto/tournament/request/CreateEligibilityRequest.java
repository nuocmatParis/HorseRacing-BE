package com.swp391.horseracing.dto.tournament.request;

import com.swp391.horseracing.enums.EligibilityTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateEligibilityRequest {

    @NotNull(message = "Target type is required")
    EligibilityTargetType targetType;

    @NotBlank(message = "Condition name is required")
    String conditionName;

    @NotBlank(message = "Condition operator is required")
    String conditionOperator;

    @NotBlank(message = "Condition value is required")
    String conditionValue;

    @NotNull(message = "Active status is required")
    Boolean isActive;
}
