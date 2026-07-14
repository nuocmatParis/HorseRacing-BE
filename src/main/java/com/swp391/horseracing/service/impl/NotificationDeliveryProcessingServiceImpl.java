package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.entity.Notification;
import com.swp391.horseracing.entity.NotificationDelivery;
import com.swp391.horseracing.enums.NotificationChannel;
import com.swp391.horseracing.enums.NotificationDeliveryStatus;
import com.swp391.horseracing.mapper.NotificationMapper;
import com.swp391.horseracing.repository.NotificationDeliveryRepository;
import com.swp391.horseracing.service.NotificationDeliveryProcessingService;
import com.swp391.horseracing.service.NotificationEmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationDeliveryProcessingServiceImpl implements NotificationDeliveryProcessingService {
    final NotificationDeliveryRepository deliveryRepository;
    final NotificationEmailService emailService;
    final SimpMessagingTemplate messagingTemplate;
    final NotificationMapper notificationMapper;

    @Value("${notification.email.enabled:true}")
    boolean emailEnabled;

    @Value("${notification.email.max-attempts:3}")
    int maxAttempts;

    @Value("${notification.email.retry-delay-minutes:5,30}")
    String retryDelayConfig;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID deliveryId) {
        NotificationDelivery delivery = deliveryRepository.findForUpdateById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() == NotificationDeliveryStatus.SENT
                || delivery.getStatus() == NotificationDeliveryStatus.SKIPPED
                || delivery.getAttemptCount() >= maxAttempts) {
            return;
        }
        if (delivery.getChannel() == NotificationChannel.EMAIL && !emailEnabled) {
            delivery.setStatus(NotificationDeliveryStatus.SKIPPED);
            delivery.setLastError("Email delivery is disabled by configuration");
            deliveryRepository.save(delivery);
            return;
        }

        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        try {
            Notification notification = delivery.getNotification();
            if (delivery.getChannel() == NotificationChannel.WEB_SOCKET) {
                NotificationResponse response = notificationMapper.toResponse(notification);
                messagingTemplate.convertAndSendToUser(
                        notification.getRecipient().getUserId().toString(), "/queue/notifications", response);
            } else if (delivery.getChannel() == NotificationChannel.EMAIL) {
                emailService.send(notification.getRecipient().getEmail(),
                        notification.getTitle(), notification.getContent());
            }
            delivery.setStatus(NotificationDeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());
            delivery.setNextRetryAt(null);
            delivery.setLastError(null);
        } catch (Exception exception) {
            delivery.setStatus(NotificationDeliveryStatus.FAILED);
            delivery.setLastError(shortError(exception));
            if (delivery.getAttemptCount() < maxAttempts) {
                delivery.setNextRetryAt(LocalDateTime.now().plusMinutes(
                        retryDelayMinutes(delivery.getAttemptCount())));
            } else {
                delivery.setNextRetryAt(null);
            }
        }
        deliveryRepository.save(delivery);
    }

    private long retryDelayMinutes(int completedAttempt) {
        List<Long> delays = new ArrayList<>();
        String[] values = retryDelayConfig.split(",");
        for (String value : values) {
            try {
                delays.add(Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed optional delay and use the fallback below.
            }
        }
        if (delays.isEmpty()) {
            return 5L;
        }
        int index = completedAttempt - 1;
        if (index >= delays.size()) {
            index = delays.size() - 1;
        }
        return delays.get(index);
    }

    private String shortError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
