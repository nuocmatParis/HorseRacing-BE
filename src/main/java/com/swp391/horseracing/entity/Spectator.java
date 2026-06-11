package com.swp391.horseracing.entity;

import jakarta.persistence.*;

@Table
@Entity
@PrimaryKeyJoinColumn(name = "UserId")
public class Spectator extends User {
    @Column(name = "TotalPoint")
    private int TotalPoints;

}
