package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.VetStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "veterinarians")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Veterinarian {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vet_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID vetId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    User user;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    String licenseNumber;

    @Column(name = "specialization", length = 100)
    String specialization;

    @Builder.Default
    @Column(name = "years_of_service", nullable = false)
    int yearsOfService = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    VetStatus status = VetStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
