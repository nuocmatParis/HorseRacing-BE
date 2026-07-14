package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Notification;
import com.swp391.horseracing.enums.NotificationEventType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient.userId = :userId
              AND n.visibleInApp = true
              AND n.archivedAt IS NULL
              AND (:isRead IS NULL OR n.read = :isRead)
              AND (:eventType IS NULL OR n.event.eventType = :eventType)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findMyNotifications(@Param("userId") UUID userId,
                                           @Param("isRead") Boolean isRead,
                                           @Param("eventType") NotificationEventType eventType,
                                           Pageable pageable);

    long countByRecipient_UserIdAndVisibleInAppTrueAndReadFalseAndArchivedAtIsNull(UUID userId);

    Optional<Notification> findByEvent_EventIdAndRecipient_UserId(UUID eventId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE n.notificationId = :notificationId")
    Optional<Notification> findForUpdateById(@Param("notificationId") UUID notificationId);

    @Modifying
    @Query("""
            UPDATE Notification n SET n.read = true, n.readAt = :readAt
            WHERE n.recipient.userId = :userId
              AND n.visibleInApp = true
              AND n.archivedAt IS NULL
              AND n.read = false
            """)
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
