package com.swp391.horseracing.dto.registration.response;

import com.swp391.horseracing.enums.RegistrationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HorseTournamentRegistrationResponse {

    UUID horseRegistrationId;
    UUID tournamentId;
    String tournamentName;
    UUID horseId;
    String horseName;
    String horseBreed;
    String horseGender;
    Integer horseAge;
    Float horseWeight;
    String horseColor;
    String horseHealthStatus;
    String horseRaceClass;
    Integer horseTotalRaces;
    Integer horseTotalWins;
    Double horseWinRate;

    UUID ownerId;
    String ownerName;
    String farmName;
    String ownerAddress;
    String licenseNumber;

    RegistrationStatus status;
    LocalDateTime submittedAt;
    UUID reviewedById;
    String reviewedByName;
    LocalDateTime reviewedAt;
    String rejectedReason;
    LocalDateTime withdrawnAt;
    String withdrawReason;
    String note;
}
