package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.wallet.request.DepositRequest;
import com.swp391.horseracing.dto.wallet.response.DepositResponse;
import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;

import java.util.List;

public interface WalletService {

    WalletResponse getMyWallet();

    Wallet createUserWallet(User user);

    List<WalletResponse> createSystemWallets();

    List<WalletResponse> getSystemWallets();

    DepositResponse deposit(DepositRequest request);
}
