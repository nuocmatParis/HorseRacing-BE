package com.swp391.horseracing.dto.violation.request;

import com.swp391.horseracing.enums.ViolationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateViolationRequest {

    String description;

    ViolationStatus status;
}
