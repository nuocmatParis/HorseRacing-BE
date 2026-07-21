package com.swp391.horseracing.dto.jockey.request;



import com.swp391.horseracing.enums.JockeyStatus;
import com.swp391.horseracing.enums.Specialization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class JockeyCreationRequest {

    @Positive(message = "Height must be positive")
    float height;

    @Positive(message = "Weight must be positive")
    float weight;

    @NotNull(message = "Specialization is required")
    Specialization specialization;

}
