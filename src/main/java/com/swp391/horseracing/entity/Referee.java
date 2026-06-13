package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RefereeStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "referees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Referee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "referee_id")
    private UUID refereeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "certification_level", length = 50)
    private String certificationLevel;

    @Builder.Default
    @Column(name = "years_of_service", nullable = false)
    private int yearsOfService = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefereeStatus status = RefereeStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
