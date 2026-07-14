package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.NotificationPreference;
import com.swp391.horseracing.enums.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    Optional<NotificationPreference> findByUser_UserIdAndEventType(UUID userId, NotificationEventType eventType);
    List<NotificationPreference> findByUser_UserIdOrderByEventTypeAsc(UUID userId);
}
