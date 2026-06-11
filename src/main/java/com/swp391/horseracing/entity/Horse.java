package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.HeathStatus;
import com.swp391.horseracing.enums.HorseBreed;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Table(name = "horse")
@Entity
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
    @Column(name = "heath_status", nullable = false)
    private HeathStatus heathStatus;
    @Column(name = "race_class", nullable = false, length = 50)
    private String raceClass;
    @Column(name = "total_races", nullable = false)
    private int totalRaces;
    @Column(name = "total_wins", nullable = false)
    private int totalWins;
    @Column(name = "win_rate", nullable = false)
    private Double winRate;
    @Column(name = "create_at")
    @CreationTimestamp
    private LocalDate createAt;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private HorseOwner owner;
}
