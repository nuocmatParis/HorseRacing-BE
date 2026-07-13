package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RaceEntryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "race_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"race_id", "lane_number"}),
        @UniqueConstraint(columnNames = {"race_id", "contract_id"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID entryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    JockeyHorseContract contract;

    @Column(name = "lane_number", nullable = false)
    int laneNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    RaceEntryStatus status = RaceEntryStatus.CONFIRMED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    LocalDateTime assignedAt;

    @Column(name = "withdrawn_at")
    LocalDateTime withdrawnAt;

    @Column(name = "withdraw_reason", columnDefinition = "TEXT")
    String withdrawReason;

    @Column(name = "scratched_reason", columnDefinition = "TEXT")
    String scratchedReason;

    @Column(name = "disqualified_at")
    LocalDateTime disqualifiedAt;

    @Column(name = "disqualified_reason", columnDefinition = "TEXT")
    String disqualifiedReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
