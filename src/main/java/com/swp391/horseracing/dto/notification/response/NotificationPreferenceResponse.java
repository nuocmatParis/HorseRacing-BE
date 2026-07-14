package com.swp391.horseracing.dto.notification.response;

import com.swp391.horseracing.enums.NotificationEventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationPreferenceResponse {
    private NotificationEventType eventType;
    private boolean inAppEnabled;
    private boolean emailEnabled;
}
