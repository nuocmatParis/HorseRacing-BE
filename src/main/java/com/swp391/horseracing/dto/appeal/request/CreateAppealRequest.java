package com.swp391.horseracing.dto.appeal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAppealRequest {

    @NotNull(message = "Entry ID is required")
    UUID entryId;

    UUID raceResultId;

    UUID relatedViolationId;

    @NotNull(message = "Category ID is required")
    UUID categoryId;

    @NotBlank(message = "Description is required")
    String description;
}
