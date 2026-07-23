package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.PrizeStatus;
import com.swp391.horseracing.enums.RaceResultStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "race_results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"race_id", "entry_id"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "result_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    RaceEntry entry;

    @Column(name = "finish_time", nullable = true)
    Float finishTime;

    @Column(name = "finish_position", nullable = true)
    Integer rank;

    @Column(name = "prize_money", precision = 15, scale = 2)
    BigDecimal prizeMoney;

    @Column(name = "owner_prize_amount", precision = 15, scale = 2)
    BigDecimal ownerPrizeAmount;

    @Column(name = "jockey_prize_amount", precision = 15, scale = 2)
    BigDecimal jockeyPrizeAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "prize_status", nullable = false, length = 20)
    PrizeStatus prizeStatus = PrizeStatus.NotEligible;

    @Builder.Default
    @Column(name = "is_prize_paid", nullable = false)
    boolean isPrizePaid = false;

    @Column(name = "prize_paid_at")
    LocalDateTime prizePaidAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    RaceResultStatus status = RaceResultStatus.FINISHED;

    @Column(name = "rating_change")
    Integer ratingChange;

    @Column(name = "rating_adjustment_reason", columnDefinition = "TEXT")
    String ratingAdjustmentReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    User recordedBy;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    LocalDateTime recordedAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
