package com.swp391.horseracing.policy;

import java.time.LocalDateTime;
import java.time.LocalTime;

public final class TournamentTimelinePolicy {

    public static final int MIN_APPROVED_ENTRIES = 8;
    public static final int REGISTRATION_BASE_DAYS = 3;
    public static final int REVIEW_DAYS = 4;
    public static final int SCHEDULING_DAYS = 4;
    public static final int PRE_RACE_BUFFER_DAYS = 2;

    private TournamentTimelinePolicy() {
    }

    public static int registrationDays(int maxApprovedEntries) {
        return REGISTRATION_BASE_DAYS + capacityLevel(maxApprovedEntries);
    }

    public static int jockeyMatchingDays(int maxApprovedEntries) {
        int level = capacityLevel(maxApprovedEntries);
        if (level == 0) {
            return 3;
        }
        return 4 + level;
    }

    public static LocalDateTime minimumRegistrationCloseAt(LocalDateTime registrationOpenAt,
                                                            int maxApprovedEntries) {
        return registrationOpenAt.plusDays(registrationDays(maxApprovedEntries));
    }

    public static LocalDateTime minimumReviewDeadlineAt(LocalDateTime registrationCloseAt) {
        return registrationCloseAt.plusDays(REVIEW_DAYS);
    }

    public static LocalDateTime minimumJockeyMatchingDeadlineAt(LocalDateTime reviewDeadlineAt,
                                                                 int maxApprovedEntries) {
        return reviewDeadlineAt.plusDays(jockeyMatchingDays(maxApprovedEntries));
    }

    public static LocalDateTime minimumSchedulingDeadlineAt(LocalDateTime jockeyMatchingDeadlineAt) {
        return jockeyMatchingDeadlineAt.plusDays(SCHEDULING_DAYS);
    }

    public static LocalDateTime competitionStartAt(LocalDateTime schedulingDeadlineAt,
                                                    LocalTime raceDayStartTime) {
        return schedulingDeadlineAt.toLocalDate()
                .plusDays(PRE_RACE_BUFFER_DAYS)
                .atTime(raceDayStartTime);
    }

    private static int capacityLevel(int maxApprovedEntries) {
        if (maxApprovedEntries < MIN_APPROVED_ENTRIES
                || (maxApprovedEntries & (maxApprovedEntries - 1)) != 0) {
            throw new IllegalArgumentException(
                    "maxApprovedEntries must be a power of two and at least 8");
        }
        return Integer.numberOfTrailingZeros(maxApprovedEntries)
                - Integer.numberOfTrailingZeros(MIN_APPROVED_ENTRIES);
    }
}
