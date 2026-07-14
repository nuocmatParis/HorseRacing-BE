package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.notification.request.UpdateNotificationPreferenceRequest;
import com.swp391.horseracing.dto.notification.response.NotificationPreferenceResponse;
import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.enums.NotificationEventType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    PageResponse<NotificationResponse> getMyNotifications(
            Boolean isRead, NotificationEventType eventType, int page, int size);
    long getUnreadCount();
    void markAsRead(UUID notificationId);
    int markAllAsRead();
    void archive(UUID notificationId);
    List<NotificationPreferenceResponse> getMyPreferences();
    NotificationPreferenceResponse updateMyPreference(
            NotificationEventType eventType, UpdateNotificationPreferenceRequest request);
}
