package com.swp391.horseracing.dto.contract.response;

import com.swp391.horseracing.enums.*;
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
public class ContractResponse {

    UUID contractId;

    UUID tournamentId;
    String tournamentName;

    UUID horseTournamentRegId;
    UUID jockeyTournamentRegId;

    UUID ownerId;
    String ownerName;

    UUID horseId;
    String horseName;

    UUID jockeyId;
    String jockeyName;

    BigDecimal hireFee;

    Float advancePercent;
    Float finalPercent;

    BigDecimal advancePaidAmount;
    BigDecimal escrowAmount;

    BigDecimal systemContractFee;

    Float ownerPrizeSharePercent;
    Float jockeyPrizeSharePercent;

    ContractPaymentStatus paymentStatus;
    EscrowStatus escrowStatus;
    AdvancePayoutStatus advancePayoutStatus;
    FinalPayoutStatus finalPayoutStatus;

    LocalDateTime advancePayoutAt;
    LocalDateTime finalPayoutAt;

    ContractStatus status;

    LocalDateTime requestedAt;
    LocalDateTime respondedAt;
    LocalDateTime acceptedAt;
    LocalDateTime submittedAt;

    UUID reviewedById;
    String reviewedByName;
    LocalDateTime reviewedAt;

    String rejectedReason;

    LocalDateTime cancelledAt;
    String cancelReason;

    LocalDateTime terminatedAt;

    String contractNote;
}
