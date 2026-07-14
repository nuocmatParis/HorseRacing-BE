package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Notification;
import com.swp391.horseracing.entity.NotificationDelivery;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.NotificationChannel;
import com.swp391.horseracing.enums.NotificationDeliveryStatus;
import com.swp391.horseracing.mapper.NotificationMapper;
import com.swp391.horseracing.repository.NotificationDeliveryRepository;
import com.swp391.horseracing.service.impl.NotificationDeliveryProcessingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryProcessingServiceTest {
    @Mock NotificationDeliveryRepository deliveryRepository;
    @Mock NotificationEmailService emailService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock NotificationMapper notificationMapper;
    @InjectMocks NotificationDeliveryProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "emailEnabled", true);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "retryDelayConfig", "5,30");
    }

    @Test
    void failedEmailIsRetriedAfterFiveMinutesWithoutThrowing() {
        UUID deliveryId = UUID.randomUUID();
        User user = User.builder().email("recipient@example.com").build();
        Notification notification = Notification.builder()
                .recipient(user).title("Tiêu đề").content("Nội dung").build();
        NotificationDelivery delivery = NotificationDelivery.builder()
                .deliveryId(deliveryId)
                .notification(notification)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationDeliveryStatus.PENDING)
                .build();
        when(deliveryRepository.findForUpdateById(deliveryId)).thenReturn(Optional.of(delivery));
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService).send(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> service.process(deliveryId));

        assertEquals(NotificationDeliveryStatus.FAILED, delivery.getStatus());
        assertEquals(1, delivery.getAttemptCount());
        assertNotNull(delivery.getNextRetryAt());
        assertTrue(delivery.getLastError().contains("SMTP unavailable"));
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void disabledEmailIsSkipped() {
        ReflectionTestUtils.setField(service, "emailEnabled", false);
        UUID deliveryId = UUID.randomUUID();
        NotificationDelivery delivery = NotificationDelivery.builder()
                .deliveryId(deliveryId)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationDeliveryStatus.PENDING)
                .build();
        when(deliveryRepository.findForUpdateById(deliveryId)).thenReturn(Optional.of(delivery));

        service.process(deliveryId);

        assertEquals(NotificationDeliveryStatus.SKIPPED, delivery.getStatus());
        verifyNoInteractions(emailService);
        verify(deliveryRepository).save(delivery);
    }
}
