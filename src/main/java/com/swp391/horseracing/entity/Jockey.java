package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.JockeyStatus;
import com.swp391.horseracing.enums.JockeyTier;
import com.swp391.horseracing.enums.Specialization;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jockeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Jockey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "jockey_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID jockeyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    User user;

    @Column(name = "height", nullable = false)
    float height;

    @Column(name = "weight", nullable = false)
    float weight;

    @Builder.Default
    @Column(name = "experience_years", nullable = false)
    int experienceYears = 0;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", nullable = false, length = 100)
    Specialization specialization;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    JockeyStatus status = JockeyStatus.AVAILABLE;

    @Builder.Default
    @Column(name = "total_races", nullable = false)
    int totalRaces = 0;

    @Builder.Default
    @Column(name = "total_wins", nullable = false)
    int totalWins = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "jockey_tier", nullable = false, length = 50)
    JockeyTier jockeyTier = JockeyTier.APPRENTICE;

    @Column(name = "tier_updated_at")
    LocalDateTime tierUpdatedAt;

    @Column(name = "last_race_at")
    LocalDateTime lastRaceAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "jockey")
    List<JockeyTournamentRegistration> tournamentRegistrations;
}