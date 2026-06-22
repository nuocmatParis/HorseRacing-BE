package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.EligibilityTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Table(name = "tournament_eligibility")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TournamentEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "eligibility_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID eligibilityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    EligibilityTargetType targetType;

    @Column(name = "condition_name", nullable = false, length = 100)
    String conditionName;

    @Column(name = "condition_operator", nullable = false, length = 10)
    String conditionOperator;

    @Column(name = "condition_value", nullable = false, length = 100)
    String conditionValue;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    boolean isActive = true;

    // ---- Relationships ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    Tournament tournament;
}
