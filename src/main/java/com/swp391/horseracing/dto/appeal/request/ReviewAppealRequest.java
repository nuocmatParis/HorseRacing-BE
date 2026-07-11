package com.swp391.horseracing.dto.appeal.request;

import com.swp391.horseracing.enums.AppealStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewAppealRequest {

    @NotNull(message = "Resolution is required")
    String resolution;

    @NotNull(message = "Status is required")
    AppealStatus status;
}
