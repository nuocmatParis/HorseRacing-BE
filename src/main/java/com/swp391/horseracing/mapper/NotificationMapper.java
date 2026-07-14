package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .eventType(notification.getEvent().getEventType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .relatedType(notification.getRelatedType())
                .relatedId(notification.getRelatedId())
                .showToast(notification.isShowToast())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
