package com.swp391.horseracing.entity;

import jakarta.persistence.*;

@Table(name = "HorseOwner")
@Entity
@PrimaryKeyJoinColumn(name = "UserId")
public class HorseOwner extends User {
    @Column(name = "FarmName", nullable = false, length = 50)
    private String farmName;
    @Column(name = "Adress", nullable = false, length = 50)
    private String adress;
    @Column(name = "LicenseNumber", nullable = false, length = 30)
    private String licenseNumber;

}
