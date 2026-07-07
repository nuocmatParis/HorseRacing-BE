package com.swp391.horseracing.dto.contract.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteRequest {

    @NotNull(message = "Tournament registration id is required")
    UUID tournamentRegistrationId;

    @NotNull(message = "Jockey tournament registration id is required")
    UUID jockeyTournamentRegistrationId;

    @NotNull(message = "Owner prize share is required")
    @DecimalMin(value = "0.0", message = "Owner prize share must be at least 0")
    Float ownerPrizeSharePercent;

    @NotNull(message = "Hire fee is required")
    @DecimalMin(value = "0.0", message = "Jockey prize share must be at least 0")
    Float jockeyPrizeSharePercent;

    String contractNote;

}
