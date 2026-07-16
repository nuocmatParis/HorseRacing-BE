package com.swp391.horseracing.simulation.persistence;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.simulation.domain.SimulationRunnerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "race_provisional_results", uniqueConstraints = @UniqueConstraint(
        name = "uk_provisional_result_entry", columnNames = {"session_id", "entry_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceProvisionalResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "provisional_result_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID provisionalResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RaceSimulationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private RaceEntry entry;

    @Column(name = "finish_position")
    private Integer finishPosition;

    @Column(name = "finish_time")
    private Double finishTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SimulationRunnerStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
