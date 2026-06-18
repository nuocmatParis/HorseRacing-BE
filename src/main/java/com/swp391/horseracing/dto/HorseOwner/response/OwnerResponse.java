package com.swp391.horseracing.dto.HorseOwner.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OwnerResponse {
    UUID userId;
    String fullName;
    String farmName;
    String address;
    String licenseNumber;
    LocalDateTime createdAt;
}
