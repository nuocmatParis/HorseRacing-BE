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
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;

    @Column(name = "license_number", length = 50)
    String licenseNumber;

    @Column(name = "specialization", length = 100)
    String specialization;

    @Column(name = "years_of_service")
    Integer yearsOfService;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    VetStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
