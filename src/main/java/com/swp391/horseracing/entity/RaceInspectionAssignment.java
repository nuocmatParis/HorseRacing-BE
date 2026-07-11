package com.swp391.horseracing.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "race_inspection_staff_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inspection_staff_race", columnNames = "race_id")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RaceInspectionAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID assignmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false, unique = true)
    Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", nullable = false)
    Veterinarian veterinarian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "med_staff_id", nullable = false)
    MedicalStaff medicalStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    LocalDateTime assignedAt;
}
