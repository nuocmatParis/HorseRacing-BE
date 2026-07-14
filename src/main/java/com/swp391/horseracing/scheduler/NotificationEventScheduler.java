package com.swp391.horseracing.scheduler;

import com.swp391.horseracing.repository.NotificationEventRepository;
import com.swp391.horseracing.service.NotificationEventProcessingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationEventScheduler {
    final NotificationEventRepository eventRepository;
    final NotificationEventProcessingService processingService;

    @Value("${notification.event.batch-size:50}")
    int batchSize;

    @Value("${notification.event.max-attempts:3}")
    int maxAttempts;

    @Scheduled(fixedDelayString = "${notification.worker.fixed-delay-ms:5000}")
    public void processEvents() {
        List<UUID> eventIds = eventRepository.findProcessableIds(
                LocalDateTime.now(), maxAttempts, PageRequest.of(0, batchSize));
        for (UUID eventId : eventIds) {
            processingService.process(eventId);
        }
    }
}
