package com.swp391.horseracing.dto.notification;

import com.swp391.horseracing.enums.NotificationEventType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationEventCommand {
    private NotificationEventType eventType;
    private String aggregateType;
    private UUID aggregateId;
    private String deduplicationKey;
    private Map<String, Object> payload;
}
