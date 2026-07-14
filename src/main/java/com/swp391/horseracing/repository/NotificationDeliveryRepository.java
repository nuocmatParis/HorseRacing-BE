package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.NotificationDelivery;
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

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    boolean existsByNotification_NotificationIdAndChannel(UUID notificationId,
                                                           com.swp391.horseracing.enums.NotificationChannel channel);

    @Query("""
            SELECT d.deliveryId FROM NotificationDelivery d
            WHERE (d.status = com.swp391.horseracing.enums.NotificationDeliveryStatus.PENDING
                OR d.status = com.swp391.horseracing.enums.NotificationDeliveryStatus.FAILED)
              AND d.attemptCount < :maxAttempts
              AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now)
            ORDER BY d.createdAt ASC
            """)
    List<UUID> findProcessableIds(@Param("now") LocalDateTime now,
                                  @Param("maxAttempts") int maxAttempts,
                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM NotificationDelivery d WHERE d.deliveryId = :deliveryId")
    Optional<NotificationDelivery> findForUpdateById(@Param("deliveryId") UUID deliveryId);

    @Modifying
    @Query("DELETE FROM NotificationDelivery d WHERE d.notification.createdAt < :cutoff")
    int deleteForNotificationsCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
