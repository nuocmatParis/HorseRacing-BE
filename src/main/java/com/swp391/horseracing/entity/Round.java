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
    @Column(name = "prediction_type", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    RoundStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    User createdBy;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL,
            orphanRemoval = true)
     List<Race> races;
}
