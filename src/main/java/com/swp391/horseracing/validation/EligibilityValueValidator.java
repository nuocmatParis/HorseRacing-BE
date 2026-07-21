package com.swp391.horseracing.validation;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateEligibilityRequest;
import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityTargetType;
import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.JockeyTier;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EligibilityValueValidator implements ConstraintValidator<ValidEligibilityValue, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        EligibilityCondition conditionName;
        EligibilityTargetType targetType;
        String conditionValue;

        if (value instanceof CreateEligibilityRequest req) {
            conditionName = req.getConditionName();
            targetType = req.getTargetType();
            conditionValue = req.getConditionValue();
        } else if (value instanceof UpdateEligibilityRequest req) {
            conditionName = req.getConditionName();
            targetType = req.getTargetType();
            conditionValue = req.getConditionValue();
            if (conditionName == null || conditionValue == null) return true;
        } else {
            return true;
        }

        if (conditionName == null || conditionValue == null) return true;

        return switch (conditionName) {
            case AGE, EXPERIENCE_YEARS -> validatePositiveInt(conditionValue, context);
            case WEIGHT -> validateWeight(targetType, conditionValue, context);
            case WIN_RATE -> validateWinRate(conditionValue, context);
            case BREED -> validateBreed(conditionValue, context);
            case JOCKEY_TIER -> validateJockeyTier(conditionValue, context);
        };
    }

    private boolean validatePositiveInt(String value, ConstraintValidatorContext context) {
        try {
            int v = Integer.parseInt(value);
            if (v < 0) {
                buildMessage(context, "Value must be a non-negative integer");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            buildMessage(context, "Value must be a valid integer");
            return false;
        }
    }

    private boolean validateWeight(EligibilityTargetType targetType, String value, ConstraintValidatorContext context) {
        try {
            float v = Float.parseFloat(value);
            if (v <= 0) {
                buildMessage(context, "Weight must be positive");
                return false;
            }
            if (targetType == EligibilityTargetType.HORSE) {
                if (v < 400 || v > 600) {
                    buildMessage(context, "Horse weight phải từ 400kg đến 600kg");
                    return false;
                }
            } else if (targetType == EligibilityTargetType.JOCKEY) {
                if (v < 45 || v > 65) {
                    buildMessage(context, "Jockey weight phải từ 45kg đến 65kg");
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            buildMessage(context, "Weight must be a valid number");
            return false;
        }
    }

    private boolean validateWinRate(String value, ConstraintValidatorContext context) {
        try {
            double v = Double.parseDouble(value);
            if (v < 0 || v > 100) {
                buildMessage(context, "Win rate must be between 0 and 100");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            buildMessage(context, "Win rate must be a valid number");
            return false;
        }
    }

    private boolean validateBreed(String value, ConstraintValidatorContext context) {
        try {
            HorseBreed.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            buildMessage(context, "Invalid horse breed: " + value);
            return false;
        }
    }

    private boolean validateJockeyTier(String value, ConstraintValidatorContext context) {
        try {
            JockeyTier.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            buildMessage(context, "Invalid jockey tier: " + value);
            return false;
        }
    }

    private void buildMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("conditionValue")
                .addConstraintViolation();
    }
}
