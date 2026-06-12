package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "rounds")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Rounds {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "round_id")
    private UUID roundId;

    @Column(name = "round_name", nullable = false, length = 100)
    private String roundName;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type")
    private PredictionType predictionType;

    @Column(name = "advancement_rule", columnDefinition = "TEXT")
    private String advancementRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoundStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournaments tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Races> races;
}
