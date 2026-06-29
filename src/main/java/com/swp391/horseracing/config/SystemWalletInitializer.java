package com.swp391.horseracing.config;

import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.repository.WalletRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemWalletInitializer implements CommandLineRunner {

    WalletRepository walletRepository;

    static final List<WalletPurpose> REQUIRED_SYSTEM_WALLETS = List.of(
            WalletPurpose.SYSTEM_REVENUE,
            WalletPurpose.SYSTEM_ESCROW,
            WalletPurpose.SYSTEM_PRIZE_POOL
    );

    @Override
    public void run(String... args) {
        for (WalletPurpose purpose : REQUIRED_SYSTEM_WALLETS) {
            walletRepository.findByOwnerTypeAndUserIsNullAndWalletPurpose(WalletOwnerType.SYSTEM, purpose)
                    .orElseGet(() -> walletRepository.save(Wallet.builder()
                            .ownerType(WalletOwnerType.SYSTEM)
                            .walletPurpose(purpose)
                            .user(null)
                            .build()));
        }
    }
}
