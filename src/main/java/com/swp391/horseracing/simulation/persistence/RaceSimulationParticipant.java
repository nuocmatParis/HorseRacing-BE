package com.swp391.horseracing.simulation.persistence;

import com.swp391.horseracing.entity.RaceEntry;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "race_simulation_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_simulation_participant_entry", columnNames = {"session_id", "entry_id"}),
        @UniqueConstraint(name = "uk_simulation_participant_lane", columnNames = {"session_id", "lane_number"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceSimulationParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "participant_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RaceSimulationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private RaceEntry entry;

    @Column(name = "horse_id", nullable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID horseId;

    @Column(name = "horse_name", nullable = false, length = 100)
    private String horseName;

    @Column(name = "horse_image_url", length = 500)
    private String horseImageUrl;

    @Column(name = "jockey_id", nullable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID jockeyId;

    @Column(name = "jockey_name", nullable = false, length = 100)
    private String jockeyName;

    @Column(name = "lane_number", nullable = false)
    private Integer laneNumber;

    @Column(name = "base_speed", nullable = false)
    private Double baseSpeed;

    @Column(name = "acceleration", nullable = false)
    private Double acceleration;

    @Column(name = "stamina", nullable = false)
    private Double stamina;

    @Column(name = "consistency_score", nullable = false)
    private Double consistency;

    @Column(name = "jockey_skill", nullable = false)
    private Double jockeySkill;

    @Column(name = "jockey_aggressiveness", nullable = false)
    private Double jockeyAggressiveness;

    @Column(name = "cornering_skill", nullable = false)
    private Double corneringSkill;

    @Column(name = "stamina_management", nullable = false)
    private Double staminaManagement;

    @Column(name = "handicap_weight")
    private Double handicapWeight;
}
