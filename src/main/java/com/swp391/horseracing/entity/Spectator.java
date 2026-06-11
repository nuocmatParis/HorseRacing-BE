package com.swp391.horseracing.entity;

import jakarta.persistence.*;

@Table(name = "spectator")
@Entity
@PrimaryKeyJoinColumn(name = "user_id")
public class Spectator extends User {
    @Column(name = "total_points")
    private int totalPoints;
}
