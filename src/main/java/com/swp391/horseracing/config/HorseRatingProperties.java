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
    private int firstBase = 6;
    private int firstMax = 12;
    private int secondBase = 2;
    private int secondMax = 5;
    private int thirdBase = 1;
    private int thirdMax = 4;
    private int fourthFifthMax = 2;
    private int dnfChange = -4;
    private int disqualifiedChange = -6;
    private int maxDecrease = -8;
    private int largeFieldSize = 8;
    private int policyVersion = 1;

    private int strongOpponentDifference = 20;
    private int mediumOpponentDifference = 10;
    private int weakOpponentDifference = 1;

    private int firstStrongOpponentBonus = 3;
    private int firstMediumOpponentBonus = 2;
    private int firstWeakOpponentBonus = 1;
    private int topThreeStrongOpponentBonus = 2;
    private int topThreeMediumOpponentBonus = 1;
    private int fourthFifthOpponentBonus = 1;

    private double winnerLargeGapPercent = 2.0;
    private double winnerMediumGapPercent = 1.0;
    private double topThreeCloseGapPercent = 0.5;
    private double fourthFifthCloseGapPercent = 2.0;

    private int winnerLargeGapBonus = 2;
    private int winnerMediumGapBonus = 1;
    private int closeFinishBonus = 1;
    private int largeFieldBonus = 1;

    private double smallUnderperformanceGapPercent = 3.0;
    private double mediumUnderperformanceGapPercent = 6.0;
    private double severeUnderperformanceGapPercent = 10.0;
    private int smallUnderperformancePenalty = -2;
    private int mediumUnderperformancePenalty = -4;
    private int severeUnderperformancePenalty = -6;
    private int highRatedUnderperformanceDifference = 20;
    private int highRatedUnderperformanceExtraPenalty = -2;

    @AssertTrue(message = "Invalid horse rating configuration")
    public boolean isValid() {
        return firstBase <= firstMax
                && secondBase <= secondMax
                && thirdBase <= thirdMax
                && fourthFifthMax >= 0
                && maxDecrease <= 0
                && dnfChange >= maxDecrease && dnfChange <= 0
                && disqualifiedChange >= maxDecrease && disqualifiedChange <= 0
                && largeFieldSize >= 2
                && winnerLargeGapPercent >= 0
                && winnerMediumGapPercent >= 0
                && topThreeCloseGapPercent >= 0
                && fourthFifthCloseGapPercent >= 0
                && smallUnderperformanceGapPercent >= 0
                && mediumUnderperformanceGapPercent >= 0
                && severeUnderperformanceGapPercent >= 0
                && strongOpponentDifference >= 0
                && mediumOpponentDifference >= 0
                && weakOpponentDifference >= 0
                && policyVersion >= 1
                && strongOpponentDifference >= mediumOpponentDifference
                && mediumOpponentDifference >= weakOpponentDifference
                && weakOpponentDifference >= 0
                && winnerLargeGapPercent >= winnerMediumGapPercent
                && severeUnderperformanceGapPercent >= mediumUnderperformanceGapPercent
                && mediumUnderperformanceGapPercent >= smallUnderperformanceGapPercent
                && firstBase >= 0 && secondBase >= 0 && thirdBase >= 0
                && firstStrongOpponentBonus >= 0
                && firstMediumOpponentBonus >= 0
                && firstWeakOpponentBonus >= 0
                && topThreeStrongOpponentBonus >= 0
                && topThreeMediumOpponentBonus >= 0
                && fourthFifthOpponentBonus >= 0
                && winnerLargeGapBonus >= 0
                && winnerMediumGapBonus >= 0
                && closeFinishBonus >= 0
                && largeFieldBonus >= 0
                && smallUnderperformancePenalty <= 0
                && mediumUnderperformancePenalty <= 0
                && severeUnderperformancePenalty <= 0
                && highRatedUnderperformanceExtraPenalty <= 0
                && severeUnderperformancePenalty <= mediumUnderperformancePenalty
                && mediumUnderperformancePenalty <= smallUnderperformancePenalty;
    }
}
