package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.mapper.TransactionMapper;
import com.swp391.horseracing.mapper.WalletMapper;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletServiceImplTest {
    private WalletMapper walletMapper;
    private WalletRepository walletRepository;
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        walletMapper = mock(WalletMapper.class);
        walletRepository = mock(WalletRepository.class);
        walletService = new WalletServiceImpl(
                walletMapper,
                walletRepository,
                mock(UserCurrentService.class),
                mock(WalletTransactionRepository.class),
                mock(TransactionMapper.class));
    }

    @Test
    void initializeSystemWalletsCreatesOnlyMissingPurposes() {
        Wallet revenue = systemWallet(WalletPurpose.SYSTEM_REVENUE);
        Wallet escrow = systemWallet(WalletPurpose.SYSTEM_ESCROW);
        Wallet prize = systemWallet(WalletPurpose.SYSTEM_PRIZE_POOL);

        when(walletRepository.findByOwnerTypeAndUserIsNullAndWalletPurpose(
                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_REVENUE))
                .thenReturn(Optional.of(revenue));
        when(walletRepository.findByOwnerTypeAndUserIsNullAndWalletPurpose(
                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_ESCROW))
                .thenReturn(Optional.of(escrow));
        when(walletRepository.findByOwnerTypeAndUserIsNullAndWalletPurpose(
                WalletOwnerType.SYSTEM, WalletPurpose.SYSTEM_PRIZE_POOL))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(prize);
        when(walletMapper.toWalletResponse(any(Wallet.class)))
                .thenAnswer(invocation -> WalletResponse.builder()
                        .walletPurpose(invocation.<Wallet>getArgument(0).getWalletPurpose())
                        .build());

        List<WalletResponse> responses = walletService.createSystemWallets();

        assertEquals(3, responses.size());
        verify(walletRepository).save(any(Wallet.class));
    }

    private Wallet systemWallet(WalletPurpose purpose) {
        return Wallet.builder()
                .ownerType(WalletOwnerType.SYSTEM)
                .walletPurpose(purpose)
                .build();
    }
}
