package com.swp391.horseracing.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.dto.notification.NotificationEventCommand;
import com.swp391.horseracing.entity.NotificationEvent;
import com.swp391.horseracing.enums.NotificationEventStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.NotificationEventRepository;
import com.swp391.horseracing.service.NotificationEventPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationEventPublisherImpl implements NotificationEventPublisher {
    NotificationEventRepository eventRepository;
    ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(NotificationEventCommand command) {
        validate(command);
        if (eventRepository.existsByDeduplicationKey(command.getDeduplicationKey())) {
            return;
        }
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .eventType(command.getEventType())
                    .aggregateType(command.getAggregateType())
                    .aggregateId(command.getAggregateId())
                    .deduplicationKey(command.getDeduplicationKey())
                    .payloadJson(objectMapper.writeValueAsString(command.getPayload()))
                    .status(NotificationEventStatus.PENDING)
                    .build();
            eventRepository.save(event);
        } catch (JacksonException exception) {
            throw new AppException(ErrorCode.NOTIFICATION_PAYLOAD_INVALID);
        }
    }

    private void validate(NotificationEventCommand command) {
        if (command == null || command.getEventType() == null || command.getAggregateId() == null
                || command.getAggregateType() == null || command.getAggregateType().isBlank()
                || command.getDeduplicationKey() == null || command.getDeduplicationKey().isBlank()
                || command.getPayload() == null) {
            throw new AppException(ErrorCode.NOTIFICATION_EVENT_INVALID);
        }
    }
}
