package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByVnpTxnRef(String vnpTxnRef);

    boolean existsByVnpTxnRef(String vnpTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentTransaction p WHERE p.vnpTxnRef = :txnRef")
    Optional<PaymentTransaction> findByVnpTxnRefForUpdate(@Param("txnRef") String vnpTxnRef);
}
