package com.swp391.horseracing.dto.notification.response;

import com.swp391.horseracing.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {

    UUID notiId;
    UUID userId;
    String title;
    String content;
    NotificationType notificationType;
    String relatedType;
    UUID relatedId;
    LocalDateTime createdAt;
    boolean isRead;
    LocalDateTime readAt;
}
