package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.enums.WalletOwnerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByUser_UserId(UUID userId);

    Optional<Wallet> findByUser_UserId(UUID userId);

    Optional<Wallet> findByOwnerTypeAndUserIsNull(WalletOwnerType walletOwnerType);

    boolean existsByOwnerTypeAndUserIsNull(WalletOwnerType walletOwnerType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findForUpdateByUser_UserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findForUpdateByWalletId(UUID walletId);



}
