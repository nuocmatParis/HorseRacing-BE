package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.notification.NotificationMessage;
import com.swp391.horseracing.entity.Notification;
import com.swp391.horseracing.entity.NotificationDelivery;
import com.swp391.horseracing.entity.NotificationEvent;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.impl.NotificationEventProcessingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventProcessingServiceTest {
    @Mock NotificationEventRepository eventRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationDeliveryRepository deliveryRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRecipientResolver recipientResolver;
    @Mock NotificationTemplateService templateService;
    @Mock NotificationPolicyService policyService;
    @InjectMocks NotificationEventProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
    }

    @Test
    void processCreatesOneNotificationAndTwoDeliveriesForOneRecipient() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        NotificationEvent event = NotificationEvent.builder()
                .eventId(eventId)
                .eventType(NotificationEventType.RACE_CANCELLED)
                .aggregateType("RACE")
                .aggregateId(UUID.randomUUID())
                .deduplicationKey("RACE_CANCELLED:test")
                .payloadJson("{}")
                .status(NotificationEventStatus.PENDING)
                .build();
        User user = User.builder().userId(userId).status(AccountStatus.ACTIVE).build();
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.add(userId);

        when(eventRepository.findForUpdateById(eventId)).thenReturn(Optional.of(event));
        when(templateService.build(event)).thenReturn(new NotificationMessage("Tiêu đề", "Nội dung", true));
        when(recipientResolver.resolve(event)).thenReturn(recipients);
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUser_UserIdAndEventType(userId, event.getEventType()))
                .thenReturn(Optional.empty());
        when(policyService.defaultInAppEnabled(event.getEventType())).thenReturn(true);
        when(policyService.defaultEmailEnabled(event.getEventType())).thenReturn(true);
        when(notificationRepository.findByEvent_EventIdAndRecipient_UserId(eventId, userId))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(UUID.randomUUID());
            return notification;
        });
        when(deliveryRepository.existsByNotification_NotificationIdAndChannel(any(), any())).thenReturn(false);

        service.process(eventId);

        assertEquals(NotificationEventStatus.PROCESSED, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        verify(notificationRepository, times(1)).save(any(Notification.class));
        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryRepository, times(2)).save(deliveryCaptor.capture());
        boolean hasWebSocket = false;
        boolean hasEmail = false;
        for (NotificationDelivery delivery : deliveryCaptor.getAllValues()) {
            if (delivery.getChannel() == NotificationChannel.WEB_SOCKET) {
                hasWebSocket = true;
            }
            if (delivery.getChannel() == NotificationChannel.EMAIL) {
                hasEmail = true;
            }
        }
        assertTrue(hasWebSocket);
        assertTrue(hasEmail);
    }

    @Test
    void retryDoesNotCreateDuplicateNotificationOrDelivery() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationEvent event = NotificationEvent.builder()
                .eventId(eventId)
                .eventType(NotificationEventType.PREDICTION_SCORED)
                .aggregateType("PREDICTION")
                .aggregateId(UUID.randomUUID())
                .deduplicationKey("PREDICTION_SCORED:test")
                .payloadJson("{}")
                .status(NotificationEventStatus.FAILED)
                .attemptCount(1)
                .build();
        User user = User.builder().userId(userId).status(AccountStatus.ACTIVE).build();
        Notification existing = Notification.builder()
                .notificationId(notificationId).event(event).recipient(user).build();
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.add(userId);

        when(eventRepository.findForUpdateById(eventId)).thenReturn(Optional.of(event));
        when(templateService.build(event)).thenReturn(new NotificationMessage("T", "C", false));
        when(recipientResolver.resolve(event)).thenReturn(recipients);
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUser_UserIdAndEventType(userId, event.getEventType()))
                .thenReturn(Optional.empty());
        when(policyService.defaultInAppEnabled(event.getEventType())).thenReturn(true);
        when(policyService.defaultEmailEnabled(event.getEventType())).thenReturn(true);
        when(notificationRepository.findByEvent_EventIdAndRecipient_UserId(eventId, userId))
                .thenReturn(Optional.of(existing));
        when(deliveryRepository.existsByNotification_NotificationIdAndChannel(notificationId, NotificationChannel.WEB_SOCKET))
                .thenReturn(true);
        when(deliveryRepository.existsByNotification_NotificationIdAndChannel(notificationId, NotificationChannel.EMAIL))
                .thenReturn(true);

        service.process(eventId);

        assertEquals(NotificationEventStatus.PROCESSED, event.getStatus());
        assertEquals(2, event.getAttemptCount());
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(deliveryRepository, never()).save(any(NotificationDelivery.class));
    }
}
