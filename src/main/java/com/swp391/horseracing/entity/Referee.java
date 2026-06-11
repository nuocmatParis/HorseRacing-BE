package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.RefereeStatus;
import jakarta.persistence.*;

@Table(name = "Referee")
@Entity
@PrimaryKeyJoinColumn(name = "UserId")
public class Referee extends User {
    @Column(name = "Certification", nullable = false,length = 50)
    private String certification;
    @Column(name = "YearOfService", nullable = false)
    private int yearOfService;
    @Column(name = "Status")
    private RefereeStatus status;

}
