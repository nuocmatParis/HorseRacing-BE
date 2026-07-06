package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.AppealStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "appeals")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appeal_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID appealId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    RaceEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_result_id")
    RaceResult raceResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_violation_id")
    Violation relatedViolation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    AppealCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id", nullable = false)
    User submittedBy;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    AppealStatus status = AppealStatus.Pending;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_referee_id")
    Referee reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @Column(name = "resolution", columnDefinition = "TEXT")
    String resolution;
}
