package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.Gender;
import com.swp391.horseracing.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.query.sqm.internal.SimpleSqmCopyContext;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "verification_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID verificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false)
    RoleName roleName;

    @Column(nullable = false, unique = true, length = 15)
    String username;

    @Column(name = "password", nullable = false)
    String password;

    @Column(nullable = false, unique = true, length = 100)
    String email;

    @Column(nullable = false)
    LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Gender gender;

    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    @Column(name = "phone_number", nullable = false, length = 20)
    String phoneNumber;

    @Column(name = "otp_code", nullable = false, length = 10)
    String otpCode;

    @Column(name = "expired_at", nullable = false)
    LocalDateTime expiredAt;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}