package com.swp391.horseracing.service;

import java.util.UUID;

public interface NotificationEventProcessingService {
    void process(UUID eventId);
}
