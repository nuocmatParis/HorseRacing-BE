package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<Transaction, UUID> {
    boolean existsByContractIdAndType(UUID contractId, TransactionType type);

    List<Transaction> findByWallet_WalletIdOrderByCreatedAtDesc(UUID walletId);

    List<Transaction> findAllByWallet_OwnerTypeAndWallet_UserIsNullOrderByCreatedAtDesc(
            WalletOwnerType ownerType);

    List<Transaction> findAllByWallet_OwnerTypeAndWallet_WalletPurposeAndWallet_UserIsNullOrderByCreatedAtDesc(
            WalletOwnerType ownerType, WalletPurpose walletPurpose
    );
}
