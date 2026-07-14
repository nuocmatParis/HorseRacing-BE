package com.swp391.horseracing.service;

import java.util.UUID;

public interface NotificationDeliveryProcessingService {
    void process(UUID deliveryId);
}
