package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RefereeStatus;
import jakarta.persistence.*;

@Table(name = "medical_staff")
@Entity
@PrimaryKeyJoinColumn(name = "user_id")
public class MedicalStaff extends User {
    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;
    @Column(name = "year_of_service", nullable = false)
    private int yearOfService;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RefereeStatus status;
}
