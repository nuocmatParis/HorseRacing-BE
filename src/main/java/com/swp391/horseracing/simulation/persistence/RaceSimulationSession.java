package com.swp391.horseracing.simulation.persistence;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.simulation.domain.SimulationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "race_simulation_sessions", uniqueConstraints =
        @UniqueConstraint(name = "uk_simulation_session_race", columnNames = "race_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceSimulationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID sessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SimulationStatus status;

    @Column(name = "random_seed", nullable = false)
    private Long randomSeed;

    @Builder.Default
    @Column(name = "current_race_time_seconds", nullable = false)
    private Double currentRaceTimeSeconds = 0.0;

    @Builder.Default
    @Column(name = "current_sequence", nullable = false)
    private Long currentSequence = 0L;

    @Column(name = "prepared_at", nullable = false)
    private LocalDateTime preparedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by", nullable = false)
    private User preparedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private User startedBy;

    @Lob
    @Column(name = "timeline_payload", columnDefinition = "LONGTEXT")
    private String timelinePayload;

    @Lob
    @Column(name = "current_snapshot_json", columnDefinition = "LONGTEXT")
    private String currentSnapshotJson;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
