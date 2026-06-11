package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RefereeStatus;
import jakarta.persistence.*;

@Table(name = "Veterinarian")
@Entity
@PrimaryKeyJoinColumn(name = "UserId")
public class Veterinarian extends User {
    @Column(name = "LicenseNumber", nullable = false, length = 50)
    private String licenseNumber;
    @Column(name = "YearOfService", nullable = false)
    private int yearOfService;
    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    private RefereeStatus Status;

}
