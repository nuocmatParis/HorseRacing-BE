package com.swp391.horseracing.dto.transaction.response;

import com.swp391.horseracing.enums.WalletPurpose;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionResponse {

    UUID transactionId;

    UUID walletId;

    UUID invoiceId;

    UUID contractId;

    UUID raceResultId;

    String type;

    String direction;

    BigDecimal amount;

    BigDecimal balanceBefore;

    BigDecimal balanceAfter;

    String counterpartyType;

    UUID transactionGroupId;

    String status;

    String note;

    WalletPurpose walletPurpose;

    LocalDateTime createdAt;
}
