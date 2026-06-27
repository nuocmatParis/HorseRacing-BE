package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.InvoiceType;
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
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invoice_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID invoiceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payer_user_id", nullable = false)
    User payerUser;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_reg_id")
    HorseTournamentRegistration horseTournamentRegistration;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jockey_tournament_reg_id")
    JockeyTournamentRegistration jockeyTournamentRegistration;

    /**
     * Jockey_Horse_Contract ch lamf.
     */
    @Column(name = "contract_id", columnDefinition = "CHAR(36)", nullable = true)
    UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 60)
    InvoiceType invoiceType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "due_date")
    LocalDateTime dueDate;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    @Column(name = "refunded_at")
    LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;
}
