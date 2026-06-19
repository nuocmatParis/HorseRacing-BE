package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.response.DepositResponse;
import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.service.WalletService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/wallets")
public class WalletController {
    WalletService walletService;

    @GetMapping("/my-wallet")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<WalletResponse> getMyWallet(){
        return ApiResponse.<WalletResponse>builder()
                .result(walletService.getMyWallet())
                .build();
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('HORSE_OWNER')")
    public ApiResponse<DepositResponse> deposit(
            @RequestBody @Valid DepositRequest request){
        return ApiResponse.<DepositResponse>builder()
                .result(walletService.deposit(request))
                .build();
    }
}
