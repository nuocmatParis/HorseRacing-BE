package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.AccountStatus;
import com.swp391.horseracing.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID userId;

    @Column(name = "username", nullable = false, length = 15, unique = true)
    String username;

    @Column(name = "password", nullable = false)
    String password;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    String email;

    @Column(name = "dob")
    LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    Gender gender;

    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    @Column(name = "phone_number", length = 20, unique = true)
    String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    AccountStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    Role role;

    @OneToOne(mappedBy = "user")
    Wallet wallet;

    @OneToMany(mappedBy = "createdBy",
            cascade = CascadeType.ALL, orphanRemoval = true)
    List<Tournaments> createdTournaments;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Round> createdRounds;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Race> createdRaces;

    @OneToMany(mappedBy = "startedBy", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Race> startedRaces;
}
