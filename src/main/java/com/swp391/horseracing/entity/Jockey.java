package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.JockeyStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jockeys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Jockey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "jockey_id")
    UUID jockeyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    User user;

    @Column(name = "height", precision = 5, scale = 2)
    BigDecimal height;

    @Column(name = "weight", precision = 5, scale = 2)
    BigDecimal weight;

    @Builder.Default
    @Column(name = "experience_years", nullable = false)
    int experienceYears = 0;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    String licenseNumber;

    @Column(name = "specialization", length = 100)
    String specialization;

    @Builder.Default
    @Column(name = "hire_fee", nullable = false, precision = 15, scale = 2)
    BigDecimal hireFee = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    JockeyStatus status = JockeyStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
