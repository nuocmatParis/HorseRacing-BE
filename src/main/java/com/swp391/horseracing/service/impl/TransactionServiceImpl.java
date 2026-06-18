package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TransactionMapper;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.TransactionService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    UserCurrentService userCurrentService;
    WalletTransactionRepository transactionRepository;
    WalletRepository walletRepository;
    TransactionMapper transactionMapper;

    @Override
    public List<TransactionResponse> getMyTransactions() {
        User currentUser = userCurrentService.getCurrentUser();

        Wallet wallet = walletRepository.findByUser_UserIdAndWalletPurpose(
                currentUser.getUserId(), WalletPurpose.USER_MAIN).
                orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        List<TransactionResponse> responseList = new ArrayList<>();

        List<Transaction> transactions = transactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(
                wallet.getWalletId());

        for(Transaction transaction : transactions){
            responseList.add(transactionMapper.toTransactionResponse(transaction));
        }

        return responseList;
    }
}
