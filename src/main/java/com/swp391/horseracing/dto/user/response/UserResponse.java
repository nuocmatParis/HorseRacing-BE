package com.swp391.horseracing.dto.user.response;

import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.RoleName;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    UUID userId;
    String username;
    String email;
    String fullName;
    String phoneNumber;
    LocalDate dob;
    Gender gender;
    AccountStatus status;
    RoleName roleName;
    LocalDateTime lastLoginAt;
}
