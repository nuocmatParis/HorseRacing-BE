package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.response.DepositResponse;
import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TransactionMapper;
import com.swp391.horseracing.mapper.WalletMapper;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.UserCurrentService;
import com.swp391.horseracing.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.swp391.horseracing.enums.WalletPurpose.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WalletServiceImpl implements WalletService {
    WalletMapper walletMapper;
    WalletRepository walletRepository;
    UserCurrentService userCurrentService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TransactionMapper transactionMapper;

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

    @Override
    @Transactional
    public DepositResponse deposit(DepositRequest request) {
        User currentUser = userCurrentService.getCurrentUser();

        Wallet wallet = walletRepository.findByUser_UserIdAndWalletPurpose(currentUser.getUserId(), USER_MAIN)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        validWallet(wallet);

        BigDecimal amount = request.getAmount();

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        wallet.setBalance(balanceAfter);

        Wallet savedWallet = walletRepository.save(wallet);

        UUID transactionGroupId = UUID.randomUUID();

        Transaction transaction = Transaction.builder()
                .wallet(savedWallet)
                .invoice(null)
                .raceResultId(null)
                .contractId(null)
                .type(TransactionType.DEPOSIT)
                .direction(TransactionDirection.CREDIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .counterpartyWalletId(null)
                .counterpartyType(CounterpartyType.EXTERNAL)
                .transactionGroupId(transactionGroupId)
                .status(TransactionStatus.SUCCESS)
                .note(request.getDescription() == null ? "Deposit money" : request.getDescription())
                .build();

        Transaction savedTransaction = walletTransactionRepository.save(transaction);

        return DepositResponse.builder()
                .walletResponse(walletMapper.toWalletResponse(savedWallet))
                .transactionResponse(transactionMapper.toTransactionResponse(savedTransaction))
                .build();
    }
}
