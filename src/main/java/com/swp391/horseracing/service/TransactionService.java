package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.enums.WalletPurpose;

import java.util.List;

public interface TransactionService {
    List<TransactionResponse> getMyTransactions();

    List<TransactionResponse> getSystemTransactions();

    List<TransactionResponse> getSystemTransactions(WalletPurpose walletPurpose);
}
