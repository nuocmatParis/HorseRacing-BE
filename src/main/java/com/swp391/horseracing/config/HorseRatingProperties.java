package com.swp391.horseracing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.AssertTrue;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "horse-rating")
public class HorseRatingProperties {
    private int firstMin = 6;
    private int firstMax = 12;
    private int secondMin = 2;
    private int secondMax = 5;
    private int thirdMin = 1;
    private int thirdMax = 4;
    private int fourthFifthMin = 0;
    private int fourthFifthMax = 2;
    private int otherMin = -8;
    private int otherMax = 0;
    private int disqualifiedMin = -8;
    private int disqualifiedMax = 0;
    private int policyVersion = 1;

    @AssertTrue(message = "Invalid horse rating configuration")
    public boolean isValid() {
        return firstMin <= firstMax
                && secondMin <= secondMax
                && thirdMin <= thirdMax
                && fourthFifthMin <= fourthFifthMax
                && otherMin <= otherMax
                && disqualifiedMin <= disqualifiedMax
                && firstMin >= 0
                && secondMin >= 0
                && thirdMin >= 0
                && fourthFifthMin >= 0
                && otherMax <= 0
                && disqualifiedMax <= 0
                && policyVersion >= 1;
    }
}
