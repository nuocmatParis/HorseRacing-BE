package com.swp391.horseracing.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentTimelinePolicyTest {

    @Test
    void registrationDaysIncreaseOneDayWheneverCapacityDoubles() {
        assertEquals(3, TournamentTimelinePolicy.registrationDays(8));
        assertEquals(4, TournamentTimelinePolicy.registrationDays(16));
        assertEquals(5, TournamentTimelinePolicy.registrationDays(32));
        assertEquals(6, TournamentTimelinePolicy.registrationDays(64));
        assertEquals(7, TournamentTimelinePolicy.registrationDays(128));
    }

    @Test
    void jockeyMatchingDaysFollowApprovedCapacityTable() {
        assertEquals(3, TournamentTimelinePolicy.jockeyMatchingDays(8));
        assertEquals(5, TournamentTimelinePolicy.jockeyMatchingDays(16));
        assertEquals(6, TournamentTimelinePolicy.jockeyMatchingDays(32));
        assertEquals(7, TournamentTimelinePolicy.jockeyMatchingDays(64));
        assertEquals(8, TournamentTimelinePolicy.jockeyMatchingDays(128));
    }

    @Test
    void competitionStartsTwoCalendarDaysAfterSchedulingDeadline() {
        LocalDateTime schedulingDeadline = LocalDateTime.of(2026, 7, 30, 18, 0);

        LocalDateTime competitionStart = TournamentTimelinePolicy.competitionStartAt(
                schedulingDeadline, LocalTime.of(8, 0));

        assertEquals(LocalDateTime.of(2026, 8, 1, 8, 0), competitionStart);
    }

    @Test
    void minimumTimelineUsesActualPreviousDeadline() {
        LocalDateTime registrationOpen = LocalDateTime.of(2026, 7, 14, 8, 0);
        LocalDateTime registrationClose = TournamentTimelinePolicy
                .minimumRegistrationCloseAt(registrationOpen, 32);
        LocalDateTime reviewDeadline = TournamentTimelinePolicy
                .minimumReviewDeadlineAt(registrationClose);
        LocalDateTime matchingDeadline = TournamentTimelinePolicy
                .minimumJockeyMatchingDeadlineAt(reviewDeadline, 32);
        LocalDateTime schedulingDeadline = TournamentTimelinePolicy
                .minimumSchedulingDeadlineAt(matchingDeadline);

        assertEquals(LocalDateTime.of(2026, 7, 19, 8, 0), registrationClose);
        assertEquals(LocalDateTime.of(2026, 7, 23, 8, 0), reviewDeadline);
        assertEquals(LocalDateTime.of(2026, 7, 29, 8, 0), matchingDeadline);
        assertEquals(LocalDateTime.of(2026, 8, 2, 8, 0), schedulingDeadline);
    }
}
