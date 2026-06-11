package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RefereeStatus;
import jakarta.persistence.*;

@Table(name = "referee")
@Entity
@PrimaryKeyJoinColumn(name = "user_id")
public class Referee extends User {
    @Column(name = "certification", nullable = false, length = 50)
    private String certification;
    @Column(name = "year_of_service", nullable = false)
    private int yearOfService;
    @Column(name = "status")
    private RefereeStatus status;
}
