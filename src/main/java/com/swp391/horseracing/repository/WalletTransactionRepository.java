package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByWallet_WalletIdOrderByCreatedAtDesc(UUID transactionId);
}
