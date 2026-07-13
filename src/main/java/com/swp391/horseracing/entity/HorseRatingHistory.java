package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RaceClass;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "horse_rating_histories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"race_result_id"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HorseRatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rating_history_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID ratingHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horse_id", nullable = false)
    Horse horse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    Race race;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_result_id", nullable = false)
    RaceResult raceResult;

    @Column(name = "old_rating", nullable = false)
    int oldRating;

    @Column(name = "base_change", nullable = false)
    int baseChange;

    @Column(name = "opponent_strength_bonus", nullable = false)
    int opponentStrengthBonus;

    @Column(name = "finish_performance_bonus", nullable = false)
    int finishPerformanceBonus;

    @Column(name = "field_size_bonus", nullable = false)
    int fieldSizeBonus;

    @Column(name = "underperformance_penalty", nullable = false)
    int underperformancePenalty;

    @Column(name = "final_change", nullable = false)
    int finalChange;

    @Column(name = "new_rating", nullable = false)
    int newRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_race_class", nullable = false, length = 50)
    RaceClass oldRaceClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_race_class", nullable = false, length = 50)
    RaceClass newRaceClass;

    @Column(name = "policy_version", nullable = false)
    int policyVersion;

    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    LocalDateTime calculatedAt;
}
