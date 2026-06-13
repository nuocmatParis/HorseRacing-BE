package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.EligibilityTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    @Column(name = "eligibility_id")
    private UUID eligibilityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private EligibilityTargetType targetType;

    @Column(name = "condition_name", nullable = false, length = 100)
    private String conditionName;

    @Column(name = "condition_operator", nullable = false, length = 10)
    private String conditionOperator;

    @Column(name = "condition_value", nullable = false, length = 100)
    private String conditionValue;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // ---- Relationships ----

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournaments tournament;
}
