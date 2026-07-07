package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.service.ContractService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
