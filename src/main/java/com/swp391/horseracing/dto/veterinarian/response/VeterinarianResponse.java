package com.swp391.horseracing.dto.veterinarian.response;

import com.swp391.horseracing.enums.VetStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VeterinarianResponse {

    UUID vetId;
    UUID userId;
    String fullName;
    String email;
    String username;
    String licenseNumber;
    String specialization;
    int yearsOfService;
    VetStatus status;
    LocalDateTime createdAt;
}
