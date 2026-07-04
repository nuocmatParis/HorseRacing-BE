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

    @NotNull(message = "Horse tournament registration id can not null")
    UUID horseTournamentRegistrationId;

    @NotNull(message = "Jockey tournament registration id can not null")
    UUID jockeyTournamentRegistrationId;


    Float advancePercent;

    Float finalPercent;

    Float ownerPrizeSharePercent;

    Float jockeyPrizeSharePercent;

    String contractNote;

}
