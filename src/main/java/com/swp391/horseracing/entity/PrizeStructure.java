package com.swp391.horseracing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Column(name = "prize_structure_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID prizeStructureId;

    @Column(name = "prize_rank", nullable = false)
    int rank;

    @Column(name = "percentage", nullable = false)
    Float percentage;

    @Column(name = "fixed_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal fixedAmount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    boolean isActive = true;

    // ---- Relationships ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    Tournament tournament;
}
