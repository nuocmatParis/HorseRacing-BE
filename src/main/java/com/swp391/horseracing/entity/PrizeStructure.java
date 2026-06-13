package com.swp391.horseracing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "prize_structures")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrizeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "prize_structure_id")
    UUID prizeStructureId;

    @Column(name = "rank", nullable = false)
    int rank;

    @Column(name = "percentage")
    Float percentage;

    @Column(name = "fixed_amount", precision = 15, scale = 2)
    BigDecimal fixedAmount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    boolean isActive = true;

    // ---- Relationships ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    Tournaments tournament;
}
