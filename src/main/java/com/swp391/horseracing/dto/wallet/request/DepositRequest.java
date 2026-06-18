package com.swp391.horseracing.dto.wallet.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Minimum deposit is 1,000 VND")
    BigDecimal amount;

    String description;
}
