package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.PredictionDetailStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Table(name = "prediction_detail")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PredictionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "prediction_detail_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID predictionDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false)
    Prediction prediction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    RaceEntry entry;

    @Column(name = "predicted_rank", nullable = false)
    int predictedRank;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    PredictionDetailStatus status = PredictionDetailStatus.UNSCORED;

    @Column(name = "awarded_points")
    Integer awardedPoints;
}
