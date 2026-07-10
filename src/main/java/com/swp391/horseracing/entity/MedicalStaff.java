package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.MedicalStaffStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_staffs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "med_staff_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID medStaffId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;

    @Column(name = "certification", nullable = false, length = 100)
    String certification;

    @Column(name = "years_of_service", nullable = false)
    Integer yearsOfService;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    MedicalStaffStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}