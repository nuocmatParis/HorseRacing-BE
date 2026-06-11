package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.AccountStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "user")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role_type", discriminatorType = DiscriminatorType.STRING)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;
    @Column(name = "user_name", nullable = false, length = 50, unique = true)
    private String userName;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "date_of_birth")
    private LocalDate dob;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus;
    @Column(name = "create_at")
    @CreationTimestamp
    private LocalDate createAt;
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
