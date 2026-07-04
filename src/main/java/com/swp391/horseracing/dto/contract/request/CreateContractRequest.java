package com.swp391.horseracing.dto.contract.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateContractRequest {

    @NotNull(message = "Tournament ID is required")
    UUID tournamentId;

    @NotNull(message = "Tournament registration ID is required")
    UUID tournamentRegId;

    @NotNull(message = "Jockey tournament registration ID is required")
    UUID jockeyTournamentRegId;

    @NotNull(message = "Owner ID is required")
    UUID ownerId;

    @NotNull(message = "Horse ID is required")
    UUID horseId;

    @NotNull(message = "Jockey ID is required")
    UUID jockeyId;

    @NotNull(message = "Hire fee is required")
    @Positive(message = "Hire fee must be positive")
    @Digits(integer = 13, fraction = 2, message = "Hire fee must have at most 13 digits and 2 decimal places")
    BigDecimal hireFee;

    @NotNull(message = "Advance percent is required")
    @DecimalMin(value = "0", message = "Advance percent must be at least 0")
    @DecimalMax(value = "100", message = "Advance percent must not exceed 100")
    Float advancePercent;

    @NotNull(message = "Final percent is required")
    @DecimalMin(value = "0", message = "Final percent must be at least 0")
    @DecimalMax(value = "100", message = "Final percent must not exceed 100")
    Float finalPercent;

    @NotNull(message = "System contract fee is required")
    @Positive(message = "System contract fee must be positive")
    @Digits(integer = 13, fraction = 2, message = "System contract fee must have at most 13 digits and 2 decimal places")
    BigDecimal systemContractFee;

    @NotNull(message = "Owner prize share percent is required")
    @DecimalMin(value = "0", message = "Owner prize share must be at least 0")
    @DecimalMax(value = "100", message = "Owner prize share must not exceed 100")
    Float ownerPrizeSharePercent;

    @NotNull(message = "Jockey prize share percent is required")
    @DecimalMin(value = "0", message = "Jockey prize share must be at least 0")
    @DecimalMax(value = "100", message = "Jockey prize share must not exceed 100")
    Float jockeyPrizeSharePercent;

    @AssertTrue(message = "Advance percent and final percent must sum to 100")
    private boolean isValidAdvanceFinalPercent() {
        if (advancePercent == null || finalPercent == null) return true;
        return Math.abs(advancePercent + finalPercent - 100.0f) < 0.01f;
    }

    @AssertTrue(message = "Owner prize share and jockey prize share must sum to 100")
    private boolean isValidPrizeSharePercent() {
        if (ownerPrizeSharePercent == null || jockeyPrizeSharePercent == null) return true;
        return Math.abs(ownerPrizeSharePercent + jockeyPrizeSharePercent - 100.0f) < 0.01f;
    }
}
