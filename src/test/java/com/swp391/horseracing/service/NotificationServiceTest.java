package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.notification.response.NotificationPreferenceResponse;
import com.swp391.horseracing.entity.Notification;
import com.swp391.horseracing.entity.NotificationPreference;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.NotificationEventType;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.mapper.NotificationMapper;
import com.swp391.horseracing.repository.NotificationPreferenceRepository;
import com.swp391.horseracing.repository.NotificationRepository;
import com.swp391.horseracing.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock NotificationMapper notificationMapper;
    @Mock NotificationPolicyService policyService;
    @Mock UserCurrentService userCurrentService;
    @InjectMocks NotificationServiceImpl service;

    @Test
    void userCannotReadAnotherUsersNotification() {
        UUID notificationId = UUID.randomUUID();
        User owner = User.builder().userId(UUID.randomUUID()).build();
        User currentUser = User.builder().userId(UUID.randomUUID()).build();
        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .recipient(owner)
                .read(false)
                .build();
        when(notificationRepository.findForUpdateById(notificationId)).thenReturn(Optional.of(notification));
        when(userCurrentService.getCurrentUser()).thenReturn(currentUser);

        assertThrows(AppException.class, () -> service.markAsRead(notificationId));

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void readingAnUnreadNotificationIsIdempotent() {
        UUID notificationId = UUID.randomUUID();
        User currentUser = User.builder().userId(UUID.randomUUID()).build();
        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .recipient(currentUser)
                .read(false)
                .build();
        when(notificationRepository.findForUpdateById(notificationId))
                .thenReturn(Optional.of(notification));
        when(userCurrentService.getCurrentUser()).thenReturn(currentUser);

        service.markAsRead(notificationId);
        service.markAsRead(notificationId);

        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void preferencesContainEveryEventAndApplySavedOverride() {
        UUID userId = UUID.randomUUID();
        User currentUser = User.builder().userId(userId).build();
        NotificationPreference override = NotificationPreference.builder()
                .user(currentUser)
                .eventType(NotificationEventType.RACE_STARTED)
                .inAppEnabled(true)
                .emailEnabled(false)
                .build();
        List<NotificationPreference> saved = new ArrayList<>();
        saved.add(override);
        when(userCurrentService.getCurrentUser()).thenReturn(currentUser);
        when(preferenceRepository.findByUser_UserIdOrderByEventTypeAsc(userId)).thenReturn(saved);
        when(policyService.defaultInAppEnabled(any())).thenReturn(true);
        when(policyService.defaultEmailEnabled(any())).thenReturn(true);

        List<NotificationPreferenceResponse> responses = service.getMyPreferences();

        assertEquals(NotificationEventType.values().length, responses.size());
        NotificationPreferenceResponse raceStarted = null;
        for (NotificationPreferenceResponse response : responses) {
            if (response.getEventType() == NotificationEventType.RACE_STARTED) {
                raceStarted = response;
                break;
            }
        }
        assertNotNull(raceStarted);
        assertTrue(raceStarted.isInAppEnabled());
        assertFalse(raceStarted.isEmailEnabled());
    }
}
