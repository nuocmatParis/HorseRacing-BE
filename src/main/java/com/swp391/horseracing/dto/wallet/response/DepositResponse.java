package com.swp391.horseracing.dto.wallet.response;

import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositResponse {

    WalletResponse walletResponse;

    TransactionResponse transactionResponse;
}
