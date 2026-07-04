package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.contract.request.CreateContractRequest;
import com.swp391.horseracing.dto.contract.request.UpdateContractRequest;
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
@RequestMapping("/api/contracts")
public class ContractController {

    ContractService contractService;

    @PostMapping
    @PreAuthorize("hasRole('HORSE_OWNER')")
    public ApiResponse<ContractResponse> create(@RequestBody @Valid CreateContractRequest request) {
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.create(request))
                .build();
    }

    @PutMapping("/{contractId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'JOCKEY')")
    public ApiResponse<ContractResponse> updateStatus(@PathVariable UUID contractId,
                                                       @RequestBody @Valid UpdateContractRequest request) {
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.updateStatus(contractId, request))
                .build();
    }

    @GetMapping("/{contractId}")
    public ApiResponse<ContractResponse> getContract(@PathVariable UUID contractId) {
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.getContractById(contractId))
                .build();
    }

    @GetMapping("/tournament/{tournamentId}")
    public ApiResponse<List<ContractResponse>> getContractsByTournament(@PathVariable UUID tournamentId) {
        return ApiResponse.<List<ContractResponse>>builder()
                .result(contractService.getContractsByTournament(tournamentId))
                .build();
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HORSE_OWNER')")
    public ApiResponse<List<ContractResponse>> getContractsByOwner(@PathVariable UUID ownerId) {
        return ApiResponse.<List<ContractResponse>>builder()
                .result(contractService.getContractsByOwner(ownerId))
                .build();
    }

    @GetMapping("/jockey/{jockeyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JOCKEY')")
    public ApiResponse<List<ContractResponse>> getContractsByJockey(@PathVariable UUID jockeyId) {
        return ApiResponse.<List<ContractResponse>>builder()
                .result(contractService.getContractsByJockey(jockeyId))
                .build();
    }
}
