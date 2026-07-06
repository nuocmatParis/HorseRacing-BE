package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void sendNotification(UUID userId, String title, String content,
                          NotificationType notificationType, String relatedType, UUID relatedId);

    List<NotificationResponse> getMyNotifications();

    void markAsRead(UUID notiId);

    int getUnreadCount();
}
