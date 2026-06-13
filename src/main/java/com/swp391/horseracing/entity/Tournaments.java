package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

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
public class Tournaments {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tournament_id")
    private UUID tournamentId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "registration_fee", precision = 15, scale = 2)
    private BigDecimal registrationFee;

    @Column(name = "jockey_registration_fee", precision = 15, scale = 2)
    private BigDecimal jockeyRegistrationFee;

    @Column(name = "system_contract_fee", precision = 15, scale = 2)
    private BigDecimal systemContractFee;

    @Column(name = "total_prize_pool", precision = 15, scale = 2)
    private BigDecimal totalPrizePool;

    @Column(name = "allowed_breed", length = 100)
    private String allowedBreed;

    @Column(name = "race_class", length = 50)
    private String raceClass;

    @Column(name = "weight_class", length = 50)
    private String weightClass;

    @Column(name = "min_horse_age")
    private int minHorseAge;

    @Column(name = "max_horse_age")
    private int maxHorseAge;

    @Column(name = "tournament_division", length = 100)
    private String tournamentDivision;

    @Column(name = "handicap_rule", columnDefinition = "TEXT")
    private String handicapRule;

    @Column(name = "prediction_open_at")
    private LocalDateTime predictionOpenAt;

    @Column(name = "prediction_close_at")
    private LocalDateTime predictionCloseAt;

    @Column(name = "prediction_reward_rule", columnDefinition = "TEXT")
    private String predictionRewardRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TournamentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private TournamentPhase phase;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrizeStructure> prizeStructures;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentEligibility> eligibilityRules;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Round> rounds;
}
