package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.contract.request.ContractRejectRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.service.ContractService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/jockey/contracts")
public class JockeyContractController {

    ContractService contractService;

    @GetMapping("/invitations")
    @PreAuthorize("hasRole('JOCKEY')")
    public ApiResponse<List<ContractResponse>> getMyInvitations(){
        return ApiResponse.<List<ContractResponse>>builder()
                .result(contractService.getMyInvitations())
                .build();
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('JOCKEY')")
    public ApiResponse<ContractResponse> acceptContract(@PathVariable UUID id){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.acceptContract(id))
                .build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('JOCKEY')")
    public ApiResponse<ContractResponse> rejectContract(@PathVariable UUID id, @RequestBody ContractRejectRequest request){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.rejectContractByJockey(id, request.getReason()))
                .build();
    }

}
