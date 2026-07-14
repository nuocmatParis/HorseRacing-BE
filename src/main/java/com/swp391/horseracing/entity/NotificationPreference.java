package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.NotificationEventType;
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
@Table(name = "notification_preferences", uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_preference_user_type", columnNames = {"user_id", "event_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "preference_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID preferenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    NotificationEventType eventType;

    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    boolean inAppEnabled = true;

    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    boolean emailEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
