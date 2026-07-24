package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.contract.request.ContractCancelRequest;
import com.swp391.horseracing.dto.contract.request.ContractRejectRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.service.ContractService;
import jakarta.validation.Valid;
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
@PreAuthorize("hasRole('JOCKEY')")
public class JockeyContractController {

    ContractService contractService;

    @GetMapping
    public ApiResponse<List<ContractResponse>> getMyContracts(){
        return ApiResponse.<List<ContractResponse>>builder()
                .result(contractService.getJockeyContracts())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractResponse> getContractById(@PathVariable UUID id){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.getJockeyContractById(id))
                .build();
    }

    @GetMapping("/invitations")
    public ApiResponse<List<ContractResponse>> getMyInvitations(){
        return ApiResponse.<List<ContractResponse>>builder()
                .result(contractService.getMyInvitations())
                .build();
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<ContractResponse> acceptContract(@PathVariable UUID id){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.acceptContract(id))
                .build();
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ContractResponse> rejectContract(@PathVariable UUID id,
                                                         @Valid @RequestBody ContractRejectRequest request){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.rejectContractByJockey(id, request.getReason()))
                .build();
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ContractResponse> cancelContract(@PathVariable UUID id,
                                                         @Valid @RequestBody ContractCancelRequest request) {
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.cancelByJockey(id, request.getReason()))
                .build();
    }

}
