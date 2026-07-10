package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.TrackCondition;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "ai_predictions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "prediction_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID predictionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false, unique = true)
    RaceEntry entry;

    @Column(name = "horse_current_rating", precision = 5, scale = 2)
    BigDecimal horseCurrentRating;

    @Column(name = "horse_recent_form", precision = 5, scale = 2)
    BigDecimal horseRecentForm;

    @Column(name = "horse_win_rate", precision = 5, scale = 2)
    BigDecimal horseWinRate;

    @Column(name = "horse_top3_rate", precision = 5, scale = 2)
    BigDecimal horseTop3Rate;

    @Column(name = "jockey_win_rate", precision = 5, scale = 2)
    BigDecimal jockeyWinRate;

    @Column(name = "jockey_top3_rate", precision = 5, scale = 2)
    BigDecimal jockeyTop3Rate;

    @Column(name = "jockey_recent_form", precision = 5, scale = 2)
    BigDecimal jockeyRecentForm;

    @Column(name = "pair_win_rate", precision = 5, scale = 2)
    BigDecimal pairWinRate;

    @Column(name = "pair_top3_rate", precision = 5, scale = 2)
    BigDecimal pairTop3Rate;

    @Column(name = "race_distance")
    int raceDistance;

    @Enumerated(EnumType.STRING)
    @Column(name = "track_condition", nullable = false, length = 20)
    TrackCondition trackCondition;

    @Column(name = "number_of_competitors")
    int numberOfCompetitors;

    @Column(name = "lane_number")
    int laneNumber;

    @Column(name = "assigned_weight_kg", precision = 5, scale = 2)
    BigDecimal assignedWeightKg;

    @Column(name = "actual_carried_weight_kg", precision = 5, scale = 2)
    BigDecimal actualCarriedWeightKg;

    @Column(name = "carried_weight_ratio", precision = 5, scale = 2)
    BigDecimal carriedWeightRatio;

    @Column(name = "relative_rating", precision = 5, scale = 2)
    BigDecimal relativeRating;

    @Column(name = "win_probability", precision = 5, scale = 2)
    BigDecimal winProbability;

    @Column(name = "predicted_top_n")
    int predictedTopN;

    @Column(name = "top_n_probability", precision = 5, scale = 2)
    BigDecimal topNProbability;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    BigDecimal confidenceScore;

    @Column(name = "prediction_reason", columnDefinition = "TEXT")
    String predictionReason;

    @Column(name = "model_version", length = 20)
    String modelVersion;

    @Column(name = "generated_at")
    LocalDateTime generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
