package com.swp391.horseracing.validation;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateEligibilityRequest;
import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityTargetType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class EligibilityTargetValidator implements ConstraintValidator<ValidEligibilityTarget, Object> {

    private static final Set<EligibilityCondition> HORSE_CONDITIONS = Set.of(
            EligibilityCondition.AGE,
            EligibilityCondition.WEIGHT,
            EligibilityCondition.BREED,
            EligibilityCondition.WIN_RATE
    );

    private static final Set<EligibilityCondition> JOCKEY_CONDITIONS = Set.of(
            EligibilityCondition.AGE,
            EligibilityCondition.WEIGHT,
            EligibilityCondition.EXPERIENCE_YEARS,
            EligibilityCondition.JOCKEY_TIER
    );

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        EligibilityCondition conditionName;
        EligibilityTargetType targetType;

        if (value instanceof CreateEligibilityRequest req) {
            conditionName = req.getConditionName();
            targetType = req.getTargetType();
        } else if (value instanceof UpdateEligibilityRequest req) {
            conditionName = req.getConditionName();
            targetType = req.getTargetType();
            if (conditionName == null || targetType == null) return true;
        } else {
            return true;
        }

        if (conditionName == null || targetType == null) return true;

        boolean compatible = switch (targetType) {
            case HORSE -> HORSE_CONDITIONS.contains(conditionName);
            case JOCKEY -> JOCKEY_CONDITIONS.contains(conditionName);
            case OWNER -> false;
        };

        if (!compatible) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Condition '" + conditionName + "' không tương thích với target type '" + targetType + "'"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
