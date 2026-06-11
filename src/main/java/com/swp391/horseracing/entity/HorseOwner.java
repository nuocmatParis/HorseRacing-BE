package com.swp391.horseracing.entity;

import jakarta.persistence.*;

@Table(name = "horse_owner")
@Entity
@PrimaryKeyJoinColumn(name = "user_id")
public class HorseOwner extends User {
    @Column(name = "farm_name", nullable = false, length = 50)
    private String farmName;
    @Column(name = "adress", nullable = false, length = 50)
    private String adress;
    @Column(name = "license_number", nullable = false, length = 30)
    private String licenseNumber;
}
