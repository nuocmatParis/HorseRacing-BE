package com.swp391.horseracing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_event_recipient", columnNames = {"event_id", "recipient_user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    NotificationEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    User recipient;

    @Column(nullable = false, length = 200)
    String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "related_type", length = 50)
    String relatedType;

    @Column(name = "related_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID relatedId;

    @Builder.Default
    @Column(name = "visible_in_app", nullable = false)
    boolean visibleInApp = true;

    @Builder.Default
    @Column(name = "show_toast", nullable = false)
    boolean showToast = true;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    boolean read = false;

    @Column(name = "read_at")
    LocalDateTime readAt;

    @Column(name = "archived_at")
    LocalDateTime archivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
