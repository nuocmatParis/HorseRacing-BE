package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.contract.request.InviteRequest;
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
@RequestMapping("/api/owner/contracts")
@PreAuthorize("hasRole('HORSE_OWNER')")
public class OwnerContractController {
    ContractService contractService;

    @GetMapping
    public ApiResponse<List<ContractResponse>> getMyContracts(){
        return ApiResponse.<List<ContractResponse>>builder()
                .result(contractService.getOwnerContracts())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractResponse> getContractById(@PathVariable UUID id){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.getOwnerContractById(id))
                .build();
    }

    @PostMapping("/invite")
    public ApiResponse<ContractResponse> inviteJockey(@RequestBody @Valid InviteRequest request){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.inviteJockey(request))
                .build();
    }

}
