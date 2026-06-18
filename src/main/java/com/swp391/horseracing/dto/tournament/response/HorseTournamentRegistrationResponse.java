package com.swp391.horseracing.dto.tournament.response;

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

    UUID registrationId;
    UUID tournamentId;
    String tournamentName;
    UUID horseId;
    String horseName;
    RegistrationStatus status;
    LocalDateTime submittedAt;
    UUID reviewedById;
    LocalDateTime reviewedAt;
    String rejectedReason;
    LocalDateTime withdrawnAt;
    String withdrawReason;
    String note;
}
