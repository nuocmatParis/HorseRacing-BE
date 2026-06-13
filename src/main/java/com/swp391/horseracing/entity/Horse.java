package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.HorseBreed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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
    private UUID horseId;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "breed", nullable = false)
    private HorseBreed breed;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;
    @Column(name = "age", nullable = false)
    private int age;
    @Column(name = "weight", nullable = false)
    private float weight;
    @Column(name = "color", nullable = false, length = 50)
    private String color;
    @Column(name = "health_status", nullable = false)
    private HealthStatus healthStatus;
    @Column(name = "race_class", nullable = false, length = 50)
    private String raceClass;
    @Column(name = "total_races", nullable = false)
    private int totalRaces;
    @Column(name = "total_wins", nullable = false)
    private int totalWins;
    @Column(name = "win_rate", nullable = false)
    private Double winRate;
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private HorseOwner owner;
}
