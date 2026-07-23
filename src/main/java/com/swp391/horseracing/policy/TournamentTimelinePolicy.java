package com.swp391.horseracing.policy;

import java.time.LocalDateTime;
import java.time.LocalTime;

public final class TournamentTimelinePolicy {

    public static final int MIN_APPROVED_ENTRIES = 8;
    public static final int DEFAULT_MAX_ENTRIES_PER_RACE = 16;
    public static final int DEFAULT_MIN_ENTRIES_PER_RACE = 8;

    private TournamentTimelinePolicy() {
    }

    public static LocalDateTime minimumRegistrationCloseAt(LocalDateTime registrationOpenAt,
                                                            int registrationDays) {
        return registrationOpenAt.plusDays(registrationDays);
    }

    public static LocalDateTime minimumReviewDeadlineAt(LocalDateTime registrationCloseAt,
                                                         int reviewDays) {
        return registrationCloseAt.plusDays(reviewDays);
    }

    public static LocalDateTime minimumJockeyMatchingDeadlineAt(LocalDateTime reviewDeadlineAt,
                                                                 int jockeyMatchingDays) {
        return reviewDeadlineAt.plusDays(jockeyMatchingDays);
    }

    public static LocalDateTime minimumSchedulingDeadlineAt(LocalDateTime jockeyMatchingDeadlineAt,
                                                             int schedulingDays) {
        return jockeyMatchingDeadlineAt.plusDays(schedulingDays);
    }

    public static LocalDateTime competitionStartAt(LocalDateTime schedulingDeadlineAt,
                                                    LocalTime raceDayStartTime,
                                                    int preRaceBufferDays) {
        return schedulingDeadlineAt.toLocalDate()
                .plusDays(preRaceBufferDays)
                .atTime(raceDayStartTime);
    }

    public static void validateMaxApprovedEntries(int maxApprovedEntries) {
        if (maxApprovedEntries < 1) {
            throw new IllegalArgumentException(
                    "maxApprovedEntries must be at least 1");
        }
    }

    public static int capacityLevel(int maxApprovedEntries) {
        return Integer.numberOfTrailingZeros(maxApprovedEntries)
                - Integer.numberOfTrailingZeros(MIN_APPROVED_ENTRIES);
    }
}
