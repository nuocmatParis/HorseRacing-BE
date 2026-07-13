package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "rounds")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "round_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID roundId;

    @Column(name = "round_name", nullable = false, length = 100)
    String roundName;

    @Column(name = "sequence_order", nullable = false)
    int sequenceOrder;

    @Column(name = "is_final", nullable = false)
    boolean isFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type", nullable = false, length = 50)
    PredictionType predictionType;

    @Column(name = "advancement_rule", columnDefinition = "TEXT", nullable = false)
    String advancementRule;

    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    LocalDateTime endDate;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    String description;

    @Column(name = "max_races", nullable = false)
    Integer maxRaces;

    @Column(name = "max_entries", nullable = false)
    int maxEntries;

    @Column(name = "min_entries", nullable = false)
    int minEntries;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    RoundStatus status;

    // Trọng tài chính duy nhất của round
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_referee_id")
    Referee headReferee;

    @Column(name = "head_referee_assigned_at")
    LocalDateTime headRefereeAssignedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    User createdBy;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Race> races;
}