package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.AccountStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "RoleType", discriminatorType = DiscriminatorType.STRING)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;
    @Column(name = "UserName", nullable = false, length = 50, unique = true)
    private String userName;
    @Column(name = "Password", nullable = false)
    private String password;
    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "DateOfBirth")
    private LocalDate dob;
    @Column(name = "FullName", nullable = false, length = 100)
    private String fullName;
    @Column(name = "PhoneNumber", nullable = false)
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "AccountStatus")
    private AccountStatus accountStatus;
    @Column(name = "CreateAt")
    @CreationTimestamp
    private LocalDate createAt;
    @Column(name = "LastLoginAt")
    private LocalDateTime lastLoginAt;
}
