package com.swp391.horseracing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "race_referees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_race_referee_one_per_race", columnNames = {"race_id"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceReferee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "race_referee_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID raceRefereeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false)
    Referee referee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    LocalDateTime assignedAt;
}
