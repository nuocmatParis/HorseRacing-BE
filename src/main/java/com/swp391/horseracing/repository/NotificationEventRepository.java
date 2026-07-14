package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.NotificationEvent;
import com.swp391.horseracing.enums.NotificationEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {
    boolean existsByDeduplicationKey(String deduplicationKey);

    @Query("""
            SELECT e.eventId FROM NotificationEvent e
            WHERE (e.status = com.swp391.horseracing.enums.NotificationEventStatus.PENDING
                OR e.status = com.swp391.horseracing.enums.NotificationEventStatus.FAILED)
              AND e.attemptCount < :maxAttempts
              AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
            ORDER BY e.createdAt ASC
            """)
    List<UUID> findProcessableIds(@Param("now") LocalDateTime now,
                                  @Param("maxAttempts") int maxAttempts,
                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM NotificationEvent e WHERE e.eventId = :eventId")
    Optional<NotificationEvent> findForUpdateById(@Param("eventId") UUID eventId);

    @Modifying
    @Query("""
            DELETE FROM NotificationEvent e
            WHERE e.status = com.swp391.horseracing.enums.NotificationEventStatus.PROCESSED
              AND e.createdAt < :cutoff
              AND NOT EXISTS (SELECT n.notificationId FROM Notification n WHERE n.event = e)
            """)
    int deleteProcessedWithoutNotificationsBefore(@Param("cutoff") LocalDateTime cutoff);
}
