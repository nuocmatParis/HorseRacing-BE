package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.PenaltyType;
import com.swp391.horseracing.enums.ViolationStatus;
import com.swp391.horseracing.enums.ViolationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "violations")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "violation_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID violationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    RaceEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false)
    Referee referee;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    ViolationType type;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 20)
    PenaltyType penaltyType;

    @Column(name = "penalty_value")
    Float penaltyValue;

    @Column(name = "occurred_at", nullable = false)
    LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    ViolationStatus status = ViolationStatus.Active;
}
