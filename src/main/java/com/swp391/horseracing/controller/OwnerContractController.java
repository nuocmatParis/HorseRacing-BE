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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/owner/contracts")
public class OwnerContractController {
    ContractService contractService;

    @PostMapping("/invite")
    @PreAuthorize("hasRole('HORSE_OWNER')")
    public ApiResponse<ContractResponse> inviteJockey(@RequestBody @Valid InviteRequest request){
        return ApiResponse.<ContractResponse>builder()
                .result(contractService.inviteJockey(request))
                .build();
    }

}
