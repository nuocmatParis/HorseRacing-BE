package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.MedicalStaffStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "med_staff_id")
    private UUID medStaffId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "certification", length = 100)
    private String certification;

    @Builder.Default
    @Column(name = "years_of_service", nullable = false)
    private int yearsOfService = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MedicalStaffStatus status = MedicalStaffStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
