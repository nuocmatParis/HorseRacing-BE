package com.swp391.horseracing.dto.appeal_category.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppealCategoryResponse {

    UUID categoryId;
    String code;
    String name;
    String description;
    boolean isActive;
    LocalDateTime createdAt;
}
