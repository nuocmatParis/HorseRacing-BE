package com.swp391.horseracing.dto.wallet.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnpayDepositResponse {

    UUID paymentTransactionId;

    String vnpTxnRef;

    BigDecimal amount;

    String currency;

    String status;

    String paymentUrl;
}
