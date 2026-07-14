package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.notification.request.UpdateNotificationPreferenceRequest;
import com.swp391.horseracing.dto.notification.response.NotificationPreferenceResponse;
import com.swp391.horseracing.dto.notification.response.NotificationResponse;
import com.swp391.horseracing.entity.Notification;
import com.swp391.horseracing.entity.NotificationPreference;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.NotificationMapper;
import com.swp391.horseracing.repository.NotificationPreferenceRepository;
import com.swp391.horseracing.repository.NotificationRepository;
import com.swp391.horseracing.service.NotificationPolicyService;
import com.swp391.horseracing.service.NotificationService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {
    NotificationRepository notificationRepository;
    NotificationPreferenceRepository preferenceRepository;
    NotificationMapper notificationMapper;
    NotificationPolicyService policyService;
    UserCurrentService userCurrentService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(
            Boolean isRead, NotificationEventType eventType, int page, int size) {
        validatePage(page, size);
        UUID userId = userCurrentService.getCurrentUser().getUserId();
        Page<Notification> notifications = notificationRepository.findMyNotifications(
                userId, isRead, eventType, PageRequest.of(page, size));
        List<NotificationResponse> items = new ArrayList<>();
        for (Notification notification : notifications.getContent()) {
            items.add(notificationMapper.toResponse(notification));
        }
        return new PageResponse<>(items, notifications.getNumber(), notifications.getSize(),
                notifications.getTotalElements(), notifications.getTotalPages(),
                notifications.isFirst(), notifications.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID userId = userCurrentService.getCurrentUser().getUserId();
        return notificationRepository
                .countByRecipient_UserIdAndVisibleInAppTrueAndReadFalseAndArchivedAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = findOwnedForUpdate(notificationId);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        UUID userId = userCurrentService.getCurrentUser().getUserId();
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void archive(UUID notificationId) {
        Notification notification = findOwnedForUpdate(notificationId);
        if (notification.getArchivedAt() == null) {
            notification.setArchivedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getMyPreferences() {
        UUID userId = userCurrentService.getCurrentUser().getUserId();
        List<NotificationPreference> savedPreferences = preferenceRepository.findByUser_UserIdOrderByEventTypeAsc(userId);
        Map<NotificationEventType, NotificationPreference> byType = new EnumMap<>(NotificationEventType.class);
        for (NotificationPreference preference : savedPreferences) {
            byType.put(preference.getEventType(), preference);
        }

        List<NotificationPreferenceResponse> responses = new ArrayList<>();
        for (NotificationEventType type : NotificationEventType.values()) {
            NotificationPreference preference = byType.get(type);
            boolean inApp = preference == null
                    ? policyService.defaultInAppEnabled(type) : preference.isInAppEnabled();
            boolean email = preference == null
                    ? policyService.defaultEmailEnabled(type) : preference.isEmailEnabled();
            responses.add(toPreferenceResponse(type, inApp, email));
        }
        return responses;
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updateMyPreference(
            NotificationEventType eventType, UpdateNotificationPreferenceRequest request) {
        User user = userCurrentService.getCurrentUser();
        NotificationPreference preference = preferenceRepository
                .findByUser_UserIdAndEventType(user.getUserId(), eventType)
                .orElseGet(() -> NotificationPreference.builder()
                        .user(user)
                        .eventType(eventType)
                        .build());
        preference.setInAppEnabled(request.getInAppEnabled());
        preference.setEmailEnabled(request.getEmailEnabled());
        preferenceRepository.save(preference);
        return toPreferenceResponse(eventType, preference.isInAppEnabled(), preference.isEmailEnabled());
    }

    private Notification findOwnedForUpdate(UUID notificationId) {
        Notification notification = notificationRepository.findForUpdateById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        UUID currentUserId = userCurrentService.getCurrentUser().getUserId();
        if (!notification.getRecipient().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return notification;
    }

    private NotificationPreferenceResponse toPreferenceResponse(
            NotificationEventType type, boolean inApp, boolean email) {
        return NotificationPreferenceResponse.builder()
                .eventType(type)
                .inAppEnabled(inApp)
                .emailEnabled(email)
                .build();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
