package com.swp391.horseracing.dto.horse.response;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.HealthStatus;
import com.swp391.horseracing.enums.HorseBreed;
import com.swp391.horseracing.enums.RaceClass;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HorseResponse {
    UUID horseId;
    String name;
    HorseBreed breed;
    Gender gender;
    int age;
    Float weight;
    String color;
    HealthStatus healthStatus;
    int currentRating;
    RaceClass raceClass;
    int highestRating;
    int totalRaces;
    int totalWins;
    int totalTop3Finishes;
    Double winRate;
    LocalDateTime lastRaceAt;
    LocalDateTime createdAt;
    UUID ownerId;
    String imageUrl;
}
