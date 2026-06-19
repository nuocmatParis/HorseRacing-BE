package com.swp391.horseracing.entity;

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

    @Column(name = "registration_fee", nullable = false, precision = 15, scale = 2)
    BigDecimal registrationFee;

    @Column(name = "system_contract_fee", nullable = false, precision = 15, scale = 2)
    BigDecimal systemContractFee;

    @Column(name = "total_prize_pool", nullable = false, precision = 15, scale = 2)
    BigDecimal totalPrizePool;

    @Column(name = "allowed_breed", nullable = false, length = 100)
    String allowedBreed;

    @Column(name = "race_class", nullable = false, length = 50)
    String raceClass;

    @Column(name = "weight_class", nullable = false, length = 50)
    String weightClass;

    @Column(name = "min_horse_age", nullable = false)
    int minHorseAge;

    @Column(name = "max_horse_age", nullable = false)
    int maxHorseAge;

    @Column(name = "tournament_division", nullable = false, length = 100)
    String tournamentDivision;

    @Column(name = "handicap_rule", nullable = false, columnDefinition = "TEXT")
    String handicapRule;

    @Column(name = "prediction_reward_rule", nullable = false, columnDefinition = "TEXT")
    String predictionRewardRule;

    @Builder.Default
    @Column(name = "prediction_open_minutes_before", nullable = false)
    int predictionOpenMinutesBefore = 120;

    @Builder.Default
    @Column(name = "prediction_close_minutes_before", nullable = false)
    int predictionCloseMinutesBefore = 5;

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
