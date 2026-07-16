package com.swp391.horseracing.simulation.persistence;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.simulation.domain.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "race_simulation_warnings", uniqueConstraints = @UniqueConstraint(
        name = "uk_simulation_warning_frame",
        columnNames = {"session_id", "entry_id", "warning_type", "sequence_number"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceSimulationWarning {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "warning_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID warningId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RaceSimulationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private RaceEntry entry;

    @Column(name = "horse_id", nullable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID horseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 40)
    private SimulationWarningType warningType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SimulationSeverity severity;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Column(name = "sequence_number", nullable = false)
    private Long sequence;

    @Column(name = "race_time_seconds", nullable = false)
    private Double raceTimeSeconds;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "suggested_action", length = 500)
    private String suggestedAction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private WarningReviewStatus reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;
}
