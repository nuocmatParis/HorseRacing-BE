package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.impl.NotificationRecipientResolverImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientResolverTest {
    @Mock UserRepository userRepository;
    @Mock HorseTournamentRegistrationRepository horseRegistrationRepository;
    @Mock JockeyTournamentRegistrationRepository jockeyRegistrationRepository;
    @Mock JockeyHorseContractRepository contractRepository;
    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock RaceInspectionStaffAssignmentRepository inspectionAssignmentRepository;
    @Mock PredictionRepository predictionRepository;
    @Mock RaceResultRepository raceResultRepository;
    @InjectMocks NotificationRecipientResolverImpl resolver;

    @Test
    void contractInvitationIsSentOnlyToInvitedJockey() {
        UUID contractId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID jockeyUserId = UUID.randomUUID();
        JockeyHorseContract contract = contract(ownerUserId, jockeyUserId);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        NotificationEvent event = event(contractId, NotificationEventType.CONTRACT_INVITED);

        Set<UUID> recipients = resolver.resolve(event);

        assertEquals(1, recipients.size());
        assertTrue(recipients.contains(jockeyUserId));
        assertFalse(recipients.contains(ownerUserId));
    }

    @Test
    void contractAcceptedIsSentOnlyToOwner() {
        UUID contractId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID jockeyUserId = UUID.randomUUID();
        JockeyHorseContract contract = contract(ownerUserId, jockeyUserId);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        NotificationEvent event = event(contractId, NotificationEventType.CONTRACT_ACCEPTED);

        Set<UUID> recipients = resolver.resolve(event);

        assertEquals(1, recipients.size());
        assertTrue(recipients.contains(ownerUserId));
        assertFalse(recipients.contains(jockeyUserId));
    }

    private JockeyHorseContract contract(UUID ownerUserId, UUID jockeyUserId) {
        User ownerUser = User.builder().userId(ownerUserId).status(AccountStatus.ACTIVE).build();
        User jockeyUser = User.builder().userId(jockeyUserId).status(AccountStatus.ACTIVE).build();
        HorseOwner owner = HorseOwner.builder().user(ownerUser).build();
        Jockey jockey = Jockey.builder().user(jockeyUser).build();
        return JockeyHorseContract.builder().owner(owner).jockey(jockey).build();
    }

    private NotificationEvent event(UUID aggregateId, NotificationEventType type) {
        return NotificationEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType("CONTRACT")
                .eventType(type)
                .build();
    }
}
