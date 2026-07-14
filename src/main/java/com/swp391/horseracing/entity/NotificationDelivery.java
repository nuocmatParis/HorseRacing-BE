package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.NotificationChannel;
import com.swp391.horseracing.enums.NotificationDeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_delivery_channel", columnNames = {"notification_id", "channel"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delivery_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID deliveryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    NotificationChannel channel;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    int attemptCount = 0;

    @Column(name = "next_retry_at")
    LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
