package com.swp391.horseracing.dto.referee.response;

import com.swp391.horseracing.enums.RefereeStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefereeResponse {

    UUID refereeId;
    UUID userId;
    String fullName;
    String email;
    String username;
    String certificationLevel;
    int yearsOfService;
    RefereeStatus status;
}
