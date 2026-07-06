package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.entity.Notification;
import com.swp391.horseracing.enums.NotificationType;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.NotificationMapper;
import com.swp391.horseracing.repository.NotificationRepository;
import com.swp391.horseracing.service.NotificationService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    NotificationRepository notificationRepository;
    NotificationMapper notificationMapper;
    UserCurrentService userCurrentService;

    @Override
    @Transactional
    public void sendNotification(UUID userId, String title, String content,
                                  NotificationType notificationType, String relatedType, UUID relatedId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .notificationType(notificationType)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        UUID currentUserId = userCurrentService.getCurrentUser().getUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notiId) {
        Notification notification = notificationRepository.findById(notiId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        UUID currentUserId = userCurrentService.getCurrentUser().getUserId();
        if (!notification.getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public int getUnreadCount() {
        UUID currentUserId = userCurrentService.getCurrentUser().getUserId();
        return notificationRepository.countByUserIdAndIsReadFalse(currentUserId);
    }
}
