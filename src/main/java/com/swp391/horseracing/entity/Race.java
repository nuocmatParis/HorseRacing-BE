package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RoundStatus;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "race_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID raceId;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "start_time")
    LocalDateTime startTime;

    @Column(name = "end_time")
    LocalDateTime endTime;

    @Column(name = "track_condition", length = 100)
    String trackCondition;

    @Column(name = "distance")
    Float distance;

    @Column(name = "max_entries")
    int maxEntries;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    RoundStatus status;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "finished_at")
    LocalDateTime finishedAt;

    @Column(name = "schedule_published_at")
    LocalDateTime schedulePublishedAt;

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
}
