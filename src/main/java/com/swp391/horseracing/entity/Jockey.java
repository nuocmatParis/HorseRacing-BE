package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.JockeyStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Table(name = "Jockey")
@Entity
@PrimaryKeyJoinColumn(name = "UserId")
public class Jockey extends User {
    @Column(name = "Height", nullable = false, length = 50)
    private float height;
    @Column(name = "Weight", nullable = false, length = 20)
    private float weight;
    @Column(name = "ExperienceYears", nullable = false, length = 20)
    private int experienceYear;
    @Column(name = "LicenseNumber", nullable = false, length = 20)
    private String licenseNumber;
    @Column(name = "Specialization", length = 100)
    private String specialization;
    @Column(name = "HireFee", nullable = false)
    private BigDecimal hireFee;
    @Enumerated(EnumType.STRING)
    @Column(name = "Status")
    private JockeyStatus status;

}
