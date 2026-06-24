package com.swp391.horseracing.dto.tournament.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePrizeStructureRequest {

    @NotNull(message = "Rank is required")
    @Min(value = 1, message = "Rank must be at least 1")
    int rank;

    @Positive(message = "Percentage must be positive")
    @DecimalMax(value = "100.00", message = "Percentage must not exceed 100")
    Float percentage;

    @Positive(message = "Fixed amount must be positive")
    @NotNull(message = "Fixed amount is Required")
    BigDecimal fixedAmount;

    @NotNull(message = "Active status is required")
    Boolean isActive;
}
