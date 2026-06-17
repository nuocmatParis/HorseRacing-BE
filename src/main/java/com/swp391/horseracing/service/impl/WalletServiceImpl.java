package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.swp391.horseracing.enums.WalletPurpose.*;

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

        Wallet wallet = walletRepository.findByUser_UserIdAndWalletPurpose(
                        currentUser.getUserId(),
                        USER_MAIN)
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
        return walletRepository.findByUser_UserIdAndWalletPurpose(user.getUserId(), USER_MAIN)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .ownerType(WalletOwnerType.USER)
                        .walletPurpose(USER_MAIN)
                        .user(user)
                        .build()));
    }


    private Wallet createSystemWalletIfNotExists(WalletPurpose walletPurpose) {
        return walletRepository.findByOwnerTypeAndUserIsNullAndWalletPurpose(WalletOwnerType.SYSTEM, walletPurpose)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .ownerType(WalletOwnerType.SYSTEM)
                        .walletPurpose(walletPurpose)
                        .user(null)
                        .build()));
    }

    @Override
    @Transactional
    public List<WalletResponse> createSystemWallets() {
        List<WalletResponse> responses = new ArrayList<>();

        boolean alreadyExists = walletRepository.existsByOwnerTypeAndUserIsNullAndWalletPurpose(
                WalletOwnerType.SYSTEM, SYSTEM_REVENUE);

        if (alreadyExists) {
            throw new AppException(ErrorCode.WALLET_ALREADY_EXISTS);
        }

        List<WalletPurpose> walletPurposes = List.of(
                SYSTEM_REVENUE,
                SYSTEM_ESCROW,
                SYSTEM_PRIZE_POOL);

        for(WalletPurpose w : walletPurposes){
            Wallet wallet = createSystemWalletIfNotExists(w);
            responses.add(walletMapper.toWalletResponse(wallet));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getSystemWallets() {
        List<WalletResponse> responses = new ArrayList<>();

        List<Wallet> wallets = walletRepository.findAllByOwnerTypeAndUserIsNullOrderByWalletPurposeAsc(WalletOwnerType.SYSTEM);

        for(Wallet w : wallets){
            responses.add(walletMapper.toWalletResponse(w));
        }

        return responses;
    }
}
