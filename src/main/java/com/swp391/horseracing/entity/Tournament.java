package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.RaceClass;
import com.swp391.horseracing.enums.RaceDistance;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Table(name = "tournaments")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tournament_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID tournamentId;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(name = "finished_at")
    LocalDateTime finishedAt;

    @Column(name = "location", nullable = false, length = 200)
    String location;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @Column(name = "registration_fee", nullable = false, precision = 15, scale = 2)
    BigDecimal registrationFee;

    @Column(name = "system_contract_fee", nullable = false, precision = 15, scale = 2)
    BigDecimal systemContractFee;

    @Column(name = "total_prize_pool", nullable = false, precision = 15, scale = 2)
    BigDecimal totalPrizePool;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_breed", nullable = false, length = 100)
    HorseBreed allowedBreed;

    @Column(name = "min_horse_age", nullable = false)
    int minHorseAge;

    @Column(name = "max_horse_age", nullable = false)
    int maxHorseAge;

    @Builder.Default
    @Column(name = "prediction_top1_correct_points", nullable = false)
    int predictionTop1CorrectPoints = 100;

    @Builder.Default
    @Column(name = "prediction_top3_exact_position_points", nullable = false)
    int predictionTop3ExactPositionPoints = 30;

    @Builder.Default
    @Column(name = "prediction_top3_correct_horse_points", nullable = false)
    int predictionTop3CorrectHorsePoints = 10;

    @Builder.Default
    @Column(name = "prediction_top3_perfect_bonus_points", nullable = false)
    int predictionTop3PerfectBonusPoints = 50;

    @Builder.Default
    @Column(name = "prediction_open_minutes_before", nullable = false)
    int predictionOpenMinutesBefore = 120;

    @Builder.Default
    @Column(name = "prediction_close_minutes_before", nullable = false)
    int predictionCloseMinutesBefore = 5;

    @Builder.Default
    @Column(name = "prediction_card_open_hours_before_first_race", nullable = false)
    int predictionCardOpenHoursBeforeFirstRace = 24;

    @Builder.Default
    @Column(name = "inspection_open_minutes_before", nullable = false)
    int inspectionOpenMinutesBefore = 90;

    @Builder.Default
    @Column(name = "inspection_close_minutes_before", nullable = false)
    int inspectionCloseMinutesBefore = 30;

    @Builder.Default
    @Column(name = "max_races_per_day", nullable = false)
    int maxRacesPerDay = 9;

    @Builder.Default
    @Column(name = "min_race_interval_minutes", nullable = false)
    int minRaceIntervalMinutes = 35;

    @Builder.Default
    @Column(name = "start_early_tolerance_minutes", nullable = false)
    int startEarlyToleranceMinutes = 0;

    @Builder.Default
    @Column(name = "start_late_tolerance_minutes", nullable = false)
    int startLateToleranceMinutes = 30;

    @Builder.Default
    @Column(name = "default_race_operational_minutes", nullable = false)
    int defaultRaceOperationalMinutes = 30;

    @Builder.Default
    @Column(name = "race_day_start_time", nullable = false)
    LocalTime raceDayStartTime = LocalTime.of(8, 0);

    @Builder.Default
    @Column(name = "race_day_end_time", nullable = false)
    LocalTime raceDayEndTime = LocalTime.of(18, 0);

    @Builder.Default
    @Column(name = "apply_break_time", nullable = false)
    Boolean applyBreakTime = false;

    @Column(name = "break_start_time")
    LocalTime breakStartTime;

    @Column(name = "break_end_time")
    LocalTime breakEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    TournamentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    TournamentPhase phase;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "registration_open_at", nullable = false)
    LocalDateTime registrationOpenAt;

    @Column(name = "registration_close_at", nullable = false)
    LocalDateTime registrationCloseAt;

    @Column(name = "review_deadline_at", nullable = false)
    LocalDateTime reviewDeadlineAt;

    @Column(name = "jockey_matching_deadline_at", nullable = false)
    LocalDateTime jockeyMatchingDeadlineAt;

    @Column(name = "scheduling_deadline_at", nullable = false)
    LocalDateTime schedulingDeadlineAt;

    @Column(name = "competition_start_at", nullable = false)
    LocalDateTime competitionStartAt;

    @Column(name = "current_round_name", length = 100)
    String currentRoundName;

    @Builder.Default
    @Column(name = "min_round_gap_days", nullable = false)
    int minRoundGapDays = 7;

    @Enumerated(EnumType.STRING)
    @Column(name = "race_class", length = 50)
    RaceClass raceClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance", nullable = false, length = 50)
    RaceDistance distance;

    @Builder.Default
    @Column(name = "top_weight_lbs")
    int topWeightLbs = 135;

    @Builder.Default
    @Column(name = "min_weight_lbs")
    int minWeightLbs = 115;

    @Builder.Default
    @Column(name = "equipment_weight_kg")
    double equipmentWeightKg = 1.5;

    @Builder.Default
    @Column(name = "handicap_enabled", nullable = false)
    boolean handicapEnabled = false;

    @Column(name = "max_approved_horses", nullable = false)
    Integer maxApprovedHorses;

    @Column(name = "max_approved_jockeys", nullable = false)
    Integer maxApprovedJockeys;

    @Column(name = "max_approved_entries", nullable = false)
    Integer maxApprovedEntries;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    User createdBy;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PrizeStructure> prizeStructures;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    List<TournamentEligibility> eligibilityRules;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Round> rounds;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    List<HorseTournamentRegistration> horseRegistrations;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    List<JockeyTournamentRegistration> jockeyRegistrations;
}
