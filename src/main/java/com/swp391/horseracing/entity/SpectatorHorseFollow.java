package com.swp391.horseracing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "spectator_horse_follows", uniqueConstraints = {
        @UniqueConstraint(name = "uk_spectator_horse_follow", columnNames = {"spectator_id", "horse_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpectatorHorseFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "follow_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID followId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spectator_id", nullable = false)
    Spectator spectator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horse_id", nullable = false)
    Horse horse;

    @CreationTimestamp
    @Column(name = "followed_at", nullable = false, updatable = false)
    LocalDateTime followedAt;
}
