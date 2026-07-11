package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.PredictionStatus;
import com.swp391.horseracing.enums.PredictionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "predictions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "prediction_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID predictionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spectator_id", nullable = false)
    Spectator spectator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    Race race;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type", nullable = false, length = 20)
    PredictionType predictionType;

    @CreationTimestamp
    @Column(name = "prediction_time", nullable = false, updatable = false)
    LocalDateTime predictionTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    PredictionStatus status = PredictionStatus.PENDING;

    @Column(name = "reward_points")
    Integer rewardPoints;

    @Column(name = "scored_at")
    LocalDateTime scoredAt;

    @Builder.Default
    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PredictionDetail> predictionDetails = new ArrayList<>();
}
