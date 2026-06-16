package com.swp391.horseracing.dto.wallet.response;

import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletStatus;
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
public class WalletResponse {

    UUID walletId;
    WalletOwnerType ownerType;
    UUID userId;
    BigDecimal balance;
    String currency;
    WalletStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
