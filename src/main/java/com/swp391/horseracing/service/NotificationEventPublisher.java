package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.notification.NotificationEventCommand;

public interface NotificationEventPublisher {
    void publish(NotificationEventCommand command);
}
