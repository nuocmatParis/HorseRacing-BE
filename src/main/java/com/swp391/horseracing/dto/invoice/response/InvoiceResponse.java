package com.swp391.horseracing.dto.invoice.response;

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
public class InvoiceResponse {
    UUID invoiceId;

    UUID payerUserId;

    UUID tournamentRegId;

    UUID jockeyTournamentRegId;

    UUID contractId;

    String invoiceType;

    BigDecimal amount;

    String status;

    LocalDateTime dueDate;

    LocalDateTime paidAt;

    LocalDateTime refundedAt;

    LocalDateTime createdAt;

    String note;
}
