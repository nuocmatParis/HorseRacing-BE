package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.service.TransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionController {
    TransactionService transactionService;

    @GetMapping("/my-transactions")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<List<TransactionResponse>> getMyTransactions(){
        return ApiResponse.<List<TransactionResponse>>builder()
                .result(transactionService.getMyTransactions())
                .build();
    }
}
