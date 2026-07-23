package com.swp391.horseracing.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TournamentTimelinePolicyTest {

    @Test
    void minimumRegistrationCloseAtAddsDays() {
        LocalDateTime openAt = LocalDateTime.of(2026, 7, 14, 8, 0);
        LocalDateTime result = TournamentTimelinePolicy.minimumRegistrationCloseAt(openAt, 5);
        assertEquals(LocalDateTime.of(2026, 7, 19, 8, 0), result);
    }

    @Test
    void minimumReviewDeadlineAtAddsDays() {
        LocalDateTime closeAt = LocalDateTime.of(2026, 7, 19, 8, 0);
        LocalDateTime result = TournamentTimelinePolicy.minimumReviewDeadlineAt(closeAt, 4);
        assertEquals(LocalDateTime.of(2026, 7, 23, 8, 0), result);
    }

    @Test
    void minimumJockeyMatchingDeadlineAtAddsDays() {
        LocalDateTime reviewAt = LocalDateTime.of(2026, 7, 23, 8, 0);
        LocalDateTime result = TournamentTimelinePolicy.minimumJockeyMatchingDeadlineAt(reviewAt, 6);
        assertEquals(LocalDateTime.of(2026, 7, 29, 8, 0), result);
    }

    @Test
    void minimumSchedulingDeadlineAtAddsDays() {
        LocalDateTime matchingAt = LocalDateTime.of(2026, 7, 29, 8, 0);
        LocalDateTime result = TournamentTimelinePolicy.minimumSchedulingDeadlineAt(matchingAt, 4);
        assertEquals(LocalDateTime.of(2026, 8, 2, 8, 0), result);
    }

    @Test
    void competitionStartsAfterBufferDays() {
        LocalDateTime schedulingDeadline = LocalDateTime.of(2026, 7, 30, 18, 0);
        LocalDateTime competitionStart = TournamentTimelinePolicy.competitionStartAt(
                schedulingDeadline, LocalTime.of(8, 0), 2);
        assertEquals(LocalDateTime.of(2026, 8, 1, 8, 0), competitionStart);
    }

    @Test
    void competitionStartsAfterCustomBufferDays() {
        LocalDateTime schedulingDeadline = LocalDateTime.of(2026, 7, 30, 18, 0);
        LocalDateTime competitionStart = TournamentTimelinePolicy.competitionStartAt(
                schedulingDeadline, LocalTime.of(8, 0), 5);
        assertEquals(LocalDateTime.of(2026, 8, 4, 8, 0), competitionStart);
    }

    @Test
    void capacityLevelReturnsZeroForMinEntries() {
        assertEquals(0, TournamentTimelinePolicy.capacityLevel(8));
        assertEquals(1, TournamentTimelinePolicy.capacityLevel(16));
        assertEquals(2, TournamentTimelinePolicy.capacityLevel(32));
        assertEquals(3, TournamentTimelinePolicy.capacityLevel(64));
        assertEquals(4, TournamentTimelinePolicy.capacityLevel(128));
    }

    @Test
    void validateMaxApprovedEntriesRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> TournamentTimelinePolicy.validateMaxApprovedEntries(0));
        assertThrows(IllegalArgumentException.class,
                () -> TournamentTimelinePolicy.validateMaxApprovedEntries(-1));
        assertDoesNotThrow(() -> TournamentTimelinePolicy.validateMaxApprovedEntries(1));
        assertDoesNotThrow(() -> TournamentTimelinePolicy.validateMaxApprovedEntries(8));
        assertDoesNotThrow(() -> TournamentTimelinePolicy.validateMaxApprovedEntries(10));
        assertDoesNotThrow(() -> TournamentTimelinePolicy.validateMaxApprovedEntries(16));
        assertDoesNotThrow(() -> TournamentTimelinePolicy.validateMaxApprovedEntries(32));
    }
}
