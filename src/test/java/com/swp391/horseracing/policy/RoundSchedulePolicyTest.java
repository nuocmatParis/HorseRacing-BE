package com.swp391.horseracing.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundSchedulePolicyTest {

    @Test
    void sevenDayGapUsesCalendarDatesInsteadOfExactTime() {
        LocalDateTime previousEnd = LocalDateTime.of(
                2026, 8, 7, 11, 45
        );
        LocalDateTime nextStart = LocalDateTime.of(
                2026, 8, 14, 8, 0
        );

        assertTrue(RoundSchedulePolicy.hasMinimumCalendarDayGap(
                previousEnd,
                nextStart,
                7
        ));
    }

    @Test
    void dateBeforeSeventhDayIsRejectedEvenWhenItsTimeIsLater() {
        LocalDateTime previousEnd = LocalDateTime.of(
                2026, 8, 7, 8, 0
        );
        LocalDateTime nextStart = LocalDateTime.of(
                2026, 8, 13, 23, 59
        );

        assertFalse(RoundSchedulePolicy.hasMinimumCalendarDayGap(
                previousEnd,
                nextStart,
                7
        ));
    }

    @Test
    void earliestDateKeepsOnlyThePreviousEndDate() {
        LocalDateTime previousEnd = LocalDateTime.of(
                2026, 8, 7, 13, 50
        );

        assertEquals(
                LocalDate.of(2026, 8, 14),
                RoundSchedulePolicy.earliestNextRoundDate(previousEnd, 7)
        );
    }
}
