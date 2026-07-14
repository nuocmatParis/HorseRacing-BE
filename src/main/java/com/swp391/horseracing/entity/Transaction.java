package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.CounterpartyType;
import com.swp391.horseracing.enums.TransactionDirection;
import com.swp391.horseracing.enums.TransactionStatus;
import com.swp391.horseracing.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @Column(name = "transaction_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    Invoice invoice;

    /**
     * RaceResult entity làm sau.
     */
    @Column(name = "race_result_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID raceResultId;

    /**
     * JockeyHorseContract entity làm sau.
     */
    @Column(name = "contract_id", columnDefinition = "CHAR(36)", nullable = true)
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    TransactionDirection direction;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    BigDecimal balanceAfter;

    @Column(name = "counterparty_wallet_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID counterpartyWalletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "counterparty_type", length = 20)
    CounterpartyType counterpartyType;

    @Column(name = "transaction_group_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID transactionGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    TransactionStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

}
