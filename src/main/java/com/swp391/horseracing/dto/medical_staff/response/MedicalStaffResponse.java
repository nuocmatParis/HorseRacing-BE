package com.swp391.horseracing.dto.medical_staff.response;

import com.swp391.horseracing.enums.MedicalStaffStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalStaffResponse {

    UUID medStaffId;
    UUID userId;
    String fullName;
    String email;
    String username;
    String certification;
    int yearsOfService;
    MedicalStaffStatus status;
    LocalDateTime createdAt;
}
