package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.violation.request.ViolationCreateRequest;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Referee;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Violation;
import com.swp391.horseracing.enums.PenaltyType;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.ViolationType;
import com.swp391.horseracing.mapper.ViolationMapper;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.ViolationRepository;
import com.swp391.horseracing.service.impl.ViolationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViolationPenaltyBehaviorTest {

    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RefereeRepository refereeRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock ViolationRepository violationRepository;
    @Mock RaceReportRepository raceReportRepository;
    @Mock UserCurrentService userCurrentService;
    @Mock ViolationMapper violationMapper;

    @InjectMocks ViolationServiceImpl violationService;

    @Test
    void warningStoresNullPenaltyValueAndKeepsEntryConfirmed() {
        TestContext context = prepareAssignedRefereeAndEntry();
        ViolationCreateRequest request = ViolationCreateRequest.builder()
                .type(ViolationType.OBSTRUCTION)
                .description("Nhắc nhở kỵ sĩ giữ đúng làn đua")
                .penaltyType(PenaltyType.WARNING)
                .build();

        violationService.createViolation(context.entryId, request);

        ArgumentCaptor<Violation> violationCaptor = ArgumentCaptor.forClass(Violation.class);
        verify(violationRepository).save(violationCaptor.capture());
        Violation savedViolation = violationCaptor.getValue();

        assertSame(context.raceEntry, savedViolation.getRaceEntry());
        assertEquals(PenaltyType.WARNING, savedViolation.getPenaltyType());
        assertNull(savedViolation.getPenaltyValue());
        assertEquals(RaceEntryStatus.CONFIRMED, context.raceEntry.getStatus());
        assertNull(context.raceEntry.getDisqualifiedAt());
        assertNull(context.raceEntry.getDisqualifiedReason());
        verify(raceEntryRepository, never()).save(any(RaceEntry.class));
    }

    @Test
    void disqualifiedStoresNullPenaltyValueAndMarksEntryDisqualified() {
        TestContext context = prepareAssignedRefereeAndEntry();
        String reason = "Can thiệp nguy hiểm vào đối thủ";
        ViolationCreateRequest request = ViolationCreateRequest.builder()
                .type(ViolationType.OBSTRUCTION)
                .description(reason)
                .penaltyType(PenaltyType.DISQUALIFIED)
                .build();

        violationService.createViolation(context.entryId, request);

        ArgumentCaptor<Violation> violationCaptor = ArgumentCaptor.forClass(Violation.class);
        verify(violationRepository).save(violationCaptor.capture());
        Violation savedViolation = violationCaptor.getValue();

        assertSame(context.raceEntry, savedViolation.getRaceEntry());
        assertEquals(PenaltyType.DISQUALIFIED, savedViolation.getPenaltyType());
        assertNull(savedViolation.getPenaltyValue());
        assertEquals(RaceEntryStatus.DISQUALIFIED, context.raceEntry.getStatus());
        assertNotNull(context.raceEntry.getDisqualifiedAt());
        assertEquals(reason, context.raceEntry.getDisqualifiedReason());
        verify(raceEntryRepository).save(context.raceEntry);
    }

    private TestContext prepareAssignedRefereeAndEntry() {
        UUID entryId = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID refereeId = UUID.randomUUID();

        Race race = Race.builder()
                .raceId(raceId)
                .status(RoundStatus.ONGOING)
                .build();
        RaceEntry raceEntry = RaceEntry.builder()
                .entryId(entryId)
                .race(race)
                .status(RaceEntryStatus.CONFIRMED)
                .build();
        User currentUser = User.builder()
                .userId(userId)
                .build();
        Referee referee = Referee.builder()
                .refereeId(refereeId)
                .status(RefereeStatus.ASSIGNED)
                .build();

        when(raceEntryRepository.findById(entryId)).thenReturn(Optional.of(raceEntry));
        when(raceReportRepository.findByRace_RaceId(raceId)).thenReturn(Optional.empty());
        when(userCurrentService.getCurrentUser()).thenReturn(currentUser);
        when(refereeRepository.findByUser_UserId(userId)).thenReturn(Optional.of(referee));
        when(raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(raceId, refereeId))
                .thenReturn(true);
        when(violationRepository.save(any(Violation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        return new TestContext(entryId, raceEntry);
    }

    private static class TestContext {
        private final UUID entryId;
        private final RaceEntry raceEntry;

        private TestContext(UUID entryId, RaceEntry raceEntry) {
            this.entryId = entryId;
            this.raceEntry = raceEntry;
        }
    }
}
