package com.swp391.horseracing.entity;

import com.swp391.horseracing.enums.InspectionResult;
import com.swp391.horseracing.enums.InspectionStatus;
import com.swp391.horseracing.enums.HorseBreed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "horse_inspections",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_horse_inspection_entry",
                        columnNames = "entry_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HorseInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "horse_inspection_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID horseInspectionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false, unique = true)
    RaceEntry raceEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", nullable = false)
    Veterinarian veterinarian;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    InspectionResult result;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "inspected_at", nullable = false)
    LocalDateTime inspectedAt;

    @Column(name = "handicap_weight")
    Float handicapWeight;

    @Column(name = "registered_weight", nullable = false)
    Float registeredWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "registered_breed", nullable = false, length = 50)
    HorseBreed registeredBreed;

    @Column(name = "actual_weight", nullable = false)
    Float actualWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "actual_breed", nullable = false, length = 50)
    HorseBreed actualBreed;

    @Column(name = "doping_detected", nullable = false)
    Boolean dopingDetected;

    @Column(name = "is_handicap_confirmed", nullable = false)
    Boolean isHandicapConfirmed;

    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    InspectionStatus status;
}
