package com.swp391.horseracing.scheduler;

import com.swp391.horseracing.repository.NotificationDeliveryRepository;
import com.swp391.horseracing.repository.NotificationRepository;
import com.swp391.horseracing.repository.NotificationEventRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationRetentionScheduler {
    final NotificationDeliveryRepository deliveryRepository;
    final NotificationRepository notificationRepository;
    final NotificationEventRepository eventRepository;

    @Value("${notification.retention-days:90}")
    int retentionDays;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void removeExpiredNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        deliveryRepository.deleteForNotificationsCreatedBefore(cutoff);
        notificationRepository.deleteCreatedBefore(cutoff);
        eventRepository.deleteProcessedWithoutNotificationsBefore(cutoff);
    }
}
