package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.JockeyStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Table(name = "jockey")
@Entity
@PrimaryKeyJoinColumn(name = "user_id")
public class Jockey extends User {
    @Column(name = "height", nullable = false)
    private float height;
    @Column(name = "weight", nullable = false)
    private float weight;
    @Column(name = "experience_years", nullable = false)
    private int experienceYear;
    @Column(name = "license_number", nullable = false, length = 20)
    private String licenseNumber;
    @Column(name = "specialization", length = 100)
    private String specialization;
    @Column(name = "hire_fee", nullable = false)
    private BigDecimal hireFee;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JockeyStatus status;
}
