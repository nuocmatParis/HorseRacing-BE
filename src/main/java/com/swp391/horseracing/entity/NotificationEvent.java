package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.NotificationEventStatus;
import com.swp391.horseracing.enums.NotificationEventType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_events", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_event_deduplication", columnNames = "deduplication_key"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    NotificationEventType eventType;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    String aggregateType;

    @Column(name = "aggregate_id", nullable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID aggregateId;

    @Column(name = "deduplication_key", nullable = false, unique = true, length = 200)
    String deduplicationKey;

    @Column(name = "payload_json", nullable = false, columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    String payloadJson;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    NotificationEventStatus status = NotificationEventStatus.PENDING;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    int attemptCount = 0;

    @Column(name = "next_retry_at")
    LocalDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "processed_at")
    LocalDateTime processedAt;
}
