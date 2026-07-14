package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.notification.NotificationMessage;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationEventProcessingServiceImpl implements NotificationEventProcessingService {
    final NotificationEventRepository eventRepository;
    final NotificationRepository notificationRepository;
    final NotificationDeliveryRepository deliveryRepository;
    final NotificationPreferenceRepository preferenceRepository;
    final UserRepository userRepository;
    final NotificationRecipientResolver recipientResolver;
    final NotificationTemplateService templateService;
    final NotificationPolicyService policyService;

    @Value("${notification.event.max-attempts:3}")
    int maxAttempts;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID eventId) {
        NotificationEvent event = eventRepository.findForUpdateById(eventId).orElse(null);
        if (event == null || event.getStatus() == NotificationEventStatus.PROCESSED
                || event.getStatus() == NotificationEventStatus.PROCESSING
                || event.getAttemptCount() >= maxAttempts) {
            return;
        }

        event.setStatus(NotificationEventStatus.PROCESSING);
        event.setAttemptCount(event.getAttemptCount() + 1);
        eventRepository.save(event);
        try {
            NotificationMessage message = templateService.build(event);
            Set<UUID> recipientIds = recipientResolver.resolve(event);
            for (UUID recipientId : recipientIds) {
                createForRecipient(event, message, recipientId);
            }
            event.setStatus(NotificationEventStatus.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
            event.setNextRetryAt(null);
            event.setLastError(null);
        } catch (Exception exception) {
            event.setStatus(NotificationEventStatus.FAILED);
            event.setLastError(shortError(exception));
            if (event.getAttemptCount() < maxAttempts) {
                event.setNextRetryAt(LocalDateTime.now().plusMinutes(1));
            } else {
                event.setNextRetryAt(null);
            }
        }
        eventRepository.save(event);
    }

    private void createForRecipient(NotificationEvent event, NotificationMessage message, UUID recipientId) {
        User recipient = userRepository.findByUserId(recipientId).orElse(null);
        if (recipient == null || recipient.getStatus() != AccountStatus.ACTIVE) {
            return;
        }
        Optional<NotificationPreference> customPreference = preferenceRepository
                .findByUser_UserIdAndEventType(recipientId, event.getEventType());
        boolean inApp = policyService.defaultInAppEnabled(event.getEventType());
        boolean email = policyService.defaultEmailEnabled(event.getEventType());
        if (customPreference.isPresent()) {
            inApp = customPreference.get().isInAppEnabled();
            email = customPreference.get().isEmailEnabled();
        }
        if (!inApp && !email) {
            return;
        }

        Notification notification = notificationRepository
                .findByEvent_EventIdAndRecipient_UserId(event.getEventId(), recipientId)
                .orElse(null);
        if (notification == null) {
            notification = Notification.builder()
                    .event(event)
                    .recipient(recipient)
                    .title(message.title())
                    .content(message.content())
                    .relatedType(event.getAggregateType())
                    .relatedId(event.getAggregateId())
                    .visibleInApp(inApp)
                    .showToast(message.showToast())
                    .read(false)
                    .build();
            notification = notificationRepository.save(notification);
        }
        if (inApp) {
            createDelivery(notification, NotificationChannel.WEB_SOCKET);
        }
        if (email) {
            createDelivery(notification, NotificationChannel.EMAIL);
        }
    }

    private void createDelivery(Notification notification, NotificationChannel channel) {
        if (deliveryRepository.existsByNotification_NotificationIdAndChannel(
                notification.getNotificationId(), channel)) {
            return;
        }
        deliveryRepository.save(NotificationDelivery.builder()
                .notification(notification)
                .channel(channel)
                .status(NotificationDeliveryStatus.PENDING)
                .attemptCount(0)
                .build());
    }

    private String shortError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
