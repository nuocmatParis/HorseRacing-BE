package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.HorseBreed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "horses")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Horse {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "horse_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID horseId;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "breed", nullable = false)
    HorseBreed breed;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    Gender gender;

    @Column(name = "age", nullable = false)
    int age;

    @Column(name = "weight", nullable = false)
    float weight;

    @Column(name = "color", nullable = false, length = 50)
    String color;

    @Column(name = "health_status", nullable = false)
    HealthStatus healthStatus;

    @Column(name = "race_class", nullable = false, length = 50)
    String raceClass;

    @Column(name = "total_races", nullable = false)
    int totalRaces;

    @Column(name = "total_wins", nullable = false)
    int totalWins;

    @Column(name = "win_rate", nullable = false)
    Double winRate;

    @Column(name = "created_at")
    @CreationTimestamp
    LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    HorseOwner owner;

    @OneToMany(mappedBy = "horse")
    List<HorseTournamentRegistration> tournamentRegistrations;
}
