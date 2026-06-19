package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/admin/wallets")
public class AdminWalletController {
    WalletService walletService;

    @PostMapping("/system/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<WalletResponse>> createSystemWallet(){
        return ApiResponse.<List<WalletResponse>>builder()
                .result(walletService.createSystemWallets())
                .build();
    }

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<WalletResponse>> getSystemWallets(){
        return ApiResponse.<List<WalletResponse>>builder()
                .result(walletService.getSystemWallets())
                .build();
    }
}
