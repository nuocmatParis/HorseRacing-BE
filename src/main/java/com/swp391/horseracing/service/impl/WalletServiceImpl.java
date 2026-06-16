package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.WalletMapper;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.service.UserCurrentService;
import com.swp391.horseracing.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WalletServiceImpl implements WalletService {
    WalletMapper walletMapper;
    WalletRepository walletRepository;
    UserCurrentService userCurrentService;

    @Override
    public WalletResponse getMyWallet() {
        User currentUser = userCurrentService.getCurrentUser();

        Wallet wallet = walletRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        return walletMapper.toWalletResponse(wallet);
    }

    private void validWallet(Wallet wallet){
        if(wallet.getStatus() == WalletStatus.FROZEN)
            throw new AppException(ErrorCode.WALLET_FROZEN);

        if(wallet.getStatus() == WalletStatus.CLOSED)
            throw new AppException(ErrorCode.WALLET_CLOSED);
    }

    @Override
    public Wallet createUserWallet(User user) {
        return walletRepository.findByUser_UserId(user.getUserId()).orElseGet(()
                -> walletRepository.save(Wallet.builder()
                .ownerType(WalletOwnerType.USER)
                .user(user)
                .build())
        );
    }
}
