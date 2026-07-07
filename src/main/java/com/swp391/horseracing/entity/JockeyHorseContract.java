package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jockey_horse_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @JoinColumn(name = "horse_tournament_reg_id", nullable = false)
    HorseTournamentRegistration horseTournamentRegistration;

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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    ContractPaymentStatus paymentStatus = ContractPaymentStatus.UNPAID;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_status", nullable = false)
    EscrowStatus escrowStatus = EscrowStatus.NOT_HELD;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "advance_payout_status", nullable = false)
    AdvancePayoutStatus advancePayoutStatus = AdvancePayoutStatus.NOT_PAID;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "final_payout_status", nullable = false)
    FinalPayoutStatus finalPayoutStatus = FinalPayoutStatus.NOT_RELEASED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ContractStatus status = ContractStatus.PENDING_JOCKEY;

    @Column(name = "advance_payout_at")
    LocalDateTime advancePayoutAt;

    @Column(name = "final_payout_at")
    LocalDateTime finalPayoutAt;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
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

    @OneToMany(mappedBy = "contract")
    @Builder.Default
    List<RaceEntry> raceEntries = new ArrayList<>();
}


