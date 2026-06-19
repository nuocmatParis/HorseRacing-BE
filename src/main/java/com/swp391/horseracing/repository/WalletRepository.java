package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByUser_UserIdAndWalletPurpose(UUID userId, WalletPurpose walletPurpose);

    Optional<Wallet> findByUser_UserIdAndWalletPurpose(UUID userId, WalletPurpose walletPurpose);

    Optional<Wallet> findByOwnerTypeAndUserIsNullAndWalletPurpose(
            WalletOwnerType walletOwnerType, WalletPurpose walletPurpose);

    boolean existsByOwnerTypeAndUserIsNullAndWalletPurpose(
            WalletOwnerType walletOwnerType, WalletPurpose walletPurpose);

    List<Wallet> findAllByOwnerTypeAndUserIsNullOrderByWalletPurposeAsc(WalletOwnerType ownerType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findForUpdateByUser_UserIdAndWalletPurpose(UUID userId, WalletPurpose walletPurpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findForUpdateByOwnerTypeAndWalletPurpose(WalletOwnerType ownerType, WalletPurpose walletPurpose);

}
