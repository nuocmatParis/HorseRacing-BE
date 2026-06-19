package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "jockey_tournament_registration",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "jockey_id"}))
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JockeyTournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "jockey_tournament_reg_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID jockeyTournamentRegId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jockey_id", nullable = false)
    Jockey jockey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    RegistrationStatus status = RegistrationStatus.PENDING_PAYMENT;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    String rejectedReason;

    @Column(name = "withdrawn_at")
    LocalDateTime withdrawnAt;

    @Column(name = "withdraw_reason", columnDefinition = "TEXT")
    String withdrawReason;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;
}
