package com.swp391.horseracing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EligibilityTargetValidator.class)
public @interface ValidEligibilityTarget {

    String message() default "Condition name is not compatible with target type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
