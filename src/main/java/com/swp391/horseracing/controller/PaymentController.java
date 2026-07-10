package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.service.ContractService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('HORSE_OWNER')")
public class PaymentController {
    ContractService contractService;

    @PostMapping("/{id}/pay-hiring-fee")
    public ApiResponse<PaymentResponse> payHiringFee(@PathVariable UUID id){
        return ApiResponse.<PaymentResponse>builder()
                .result(contractService.payHiringFee(id))
                .build();
    }

    @PostMapping("/{id}/pay-contract-fee")
    public ApiResponse<PaymentResponse> payContractFee(@PathVariable UUID id){
        return ApiResponse.<PaymentResponse>builder()
                .result(contractService.payContractCreationFee(id))
                .build();
    }
}
