package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.service.TransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/admin/transactions")
public class AdminTransactionController {
    TransactionService transactionService;

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TransactionResponse>> getSystemTransactions(){
        return ApiResponse.<List<TransactionResponse>>builder()
                .result(transactionService.getSystemTransactions())
                .build();
    }

    @GetMapping("/system/{purpose}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TransactionResponse>> getSystemTransactions(
            @PathVariable String purpose
            ){
        WalletPurpose walletPurpose = WalletPurpose.valueOf(purpose.toUpperCase());

        return ApiResponse.<List<TransactionResponse>>builder()
                .result(transactionService.getSystemTransactions(walletPurpose))
                .build();
    }

}
