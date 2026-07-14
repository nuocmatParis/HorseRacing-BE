package com.swp391.horseracing.dto.notification.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.swp391.horseracing.enums.NotificationEventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private UUID notificationId;
    private NotificationEventType eventType;
    private String title;
    private String content;
    private String relatedType;
    private UUID relatedId;
    private boolean showToast;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
