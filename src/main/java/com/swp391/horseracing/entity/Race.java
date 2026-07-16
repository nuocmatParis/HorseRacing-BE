package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RaceDistance;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.AIPredictionPublicationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "races")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults( level = AccessLevel.PRIVATE)
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "race_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID raceId;

    @Column(name = "name", nullable = false, length = 150, unique = true)
    String name;

    @Column(name = "start_time")
    LocalDateTime startTime;

    @Column(name = "end_time")
    LocalDateTime endTime;

    @Column(name = "track_condition", nullable = false, length = 100)
    String trackCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance", nullable = false, length = 50)
    RaceDistance distance;

    @Column(name = "sequence_order", nullable = false)
    int sequenceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    RoundStatus status;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "finished_at")
    LocalDateTime finishedAt;

    @Column(name = "schedule_published_at")
    LocalDateTime schedulePublishedAt;

    @Column(name = "prediction_open_at")
    LocalDateTime predictionOpenAt;

    @Column(name = "prediction_close_at")
    LocalDateTime predictionCloseAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_prediction_publication_status", length = 20)
    AIPredictionPublicationStatus aiPredictionPublicationStatus;

    @Column(name = "ai_prediction_generated_at")
    LocalDateTime aiPredictionGeneratedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_prediction_generated_by")
    User aiPredictionGeneratedBy;

    @Column(name = "ai_prediction_published_at")
    LocalDateTime aiPredictionPublishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_prediction_published_by")
    User aiPredictionPublishedBy;

    // ---- Relationships ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private User startedBy;

    @Column(name = "inspection_finalized_at")
    LocalDateTime inspectionFinalizedAt;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    String cancellationReason;

    @Column(name = "rescheduled_at")
    LocalDateTime rescheduledAt;

    @Column(name = "reschedule_reason")
    String rescheduleReason;
}
