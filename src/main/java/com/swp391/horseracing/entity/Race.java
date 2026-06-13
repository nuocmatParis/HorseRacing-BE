package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    @Column(name = "race_id")
    private UUID raceId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "track_condition", length = 100)
    private String trackCondition;

    @Column(name = "distance")
    private Float distance;

    @Column(name = "max_entries")
    private int maxEntries;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoundStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "schedule_published_at")
    private LocalDateTime schedulePublishedAt;

    // ---- Relationships ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private User startedBy;
}
