package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.ContractPaymentStatus;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.EscrowStatus;
import com.swp391.horseracing.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jockey_horse_contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JockeyHorseContract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contract_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID contractId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_reg_id", nullable = false)
    HorseTournamentRegistration tournamentRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jockey_tournament_reg_id", nullable = false)
    JockeyTournamentRegistration jockeyTournamentRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    HorseOwner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horse_id", nullable = false)
    Horse horse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jockey_id", nullable = false)
    Jockey jockey;

    @Column(name = "hire_fee", nullable = false, precision = 15, scale = 2)
    BigDecimal hireFee;

    @Column(name = "advance_percent", nullable = false)
    Float advancePercent;

    @Column(name = "final_percent", nullable = false)
    Float finalPercent;

    @Column(name = "advance_paid_amount", precision = 15, scale = 2)
    BigDecimal advancePaidAmount;

    @Column(name = "escrow_amount", precision = 15, scale = 2)
    BigDecimal escrowAmount;

    @Column(name = "system_contract_fee", nullable = false, precision = 15, scale = 2)
    BigDecimal systemContractFee;

    @Column(name = "owner_prize_share_percent", nullable = false)
    Float ownerPrizeSharePercent;

    @Column(name = "jockey_prize_share_percent", nullable = false)
    Float jockeyPrizeSharePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    ContractPaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", nullable = false)
    EscrowStatus escrowStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "advance_payout_status", nullable = false)
    PayoutStatus payoutStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_payout_status", nullable = false)
    PayoutStatus finalPayoutStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ContractStatus status;

    @Column(name = "advance_payout_at")
    LocalDateTime advancePayoutAt;

    @Column(name = "final_payout_at")
    LocalDateTime finalPayoutAt;

    @Column(name = "requested_at", nullable = false)
    LocalDateTime requestedAt;

    @Column(name = "responded_at")
    LocalDateTime respondedAt;

    @Column(name = "accepted_at")
    LocalDateTime acceptedAt;

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    String rejectedReason;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    String cancelReason;

    @Column(name = "terminated_at")
    LocalDateTime terminatedAt;

    @Column(name = "contract_note", columnDefinition = "TEXT")
    String contractNote;
}


