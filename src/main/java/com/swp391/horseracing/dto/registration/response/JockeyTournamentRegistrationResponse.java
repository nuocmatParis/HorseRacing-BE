package com.swp391.horseracing.dto.registration.response;

import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.Specialization;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JockeyTournamentRegistrationResponse {

    UUID jockeyTournamentRegId;
    UUID tournamentId;
    String tournamentName;
    UUID jockeyId;
    String jockeyName;
    float height;
    float weight;
    int experienceYears;
    Specialization specialization;
    BigDecimal hireFee;
    String jockeyStatus;
    String email;
    String phoneNumber;
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
