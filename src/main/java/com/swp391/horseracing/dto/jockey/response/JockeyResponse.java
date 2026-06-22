package com.swp391.horseracing.dto.jockey.response;

import com.swp391.horseracing.enums.JockeyStatus;
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
    String licenseNumber;
    String specialization;
    BigDecimal hireFee ;
    JockeyStatus status ;
    LocalDateTime createdAt;
}
