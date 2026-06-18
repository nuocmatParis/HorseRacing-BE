package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.transaction.response.TransactionResponse;

import java.util.List;

public interface TransactionService {
    List<TransactionResponse> getMyTransactions();
}
