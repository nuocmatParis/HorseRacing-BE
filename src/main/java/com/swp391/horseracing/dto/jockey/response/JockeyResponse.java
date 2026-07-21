package com.swp391.horseracing.dto.jockey.response;

import com.swp391.horseracing.enums.JockeyStatus;
import com.swp391.horseracing.enums.JockeyTier;
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
public class JockeyResponse {
    UUID jockeyId;
    UUID userId;
    String fullName;
    float height;
    float weight;
    int experienceYears ;
    Specialization specialization;
    JockeyStatus status ;
    LocalDateTime createdAt;
    int totalRaces;
    int totalWins;
    JockeyTier jockeyTier;
    LocalDateTime tierUpdatedAt;
    LocalDateTime lastRaceAt;
    String imageUrl;
}
