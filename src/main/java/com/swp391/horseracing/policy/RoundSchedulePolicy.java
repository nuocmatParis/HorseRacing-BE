package com.swp391.horseracing.policy;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class RoundSchedulePolicy {

    private RoundSchedulePolicy() {
    }

    public static LocalDate earliestNextRoundDate(
            LocalDateTime previousRoundEnd,
            int minimumGapDays) {
        return previousRoundEnd.toLocalDate().plusDays(minimumGapDays);
    }

    public static boolean hasMinimumCalendarDayGap(
            LocalDateTime previousRoundEnd,
            LocalDateTime nextRoundStart,
            int minimumGapDays) {
        LocalDate earliestDate = earliestNextRoundDate(
                previousRoundEnd,
                minimumGapDays
        );

        return !nextRoundStart.toLocalDate().isBefore(earliestDate);
    }
}
