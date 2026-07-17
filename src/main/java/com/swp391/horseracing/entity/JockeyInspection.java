package com.swp391.horseracing.entity;
import com.swp391.horseracing.enums.InspectionResult;
import com.swp391.horseracing.enums.InspectionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "jockey_inspections",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_jockey_inspection_entry",
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
public class JockeyInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "jockey_inspection_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    UUID jockeyInspectionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false, unique = true)
    RaceEntry raceEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "med_staff_id", nullable = false)
    MedicalStaff medicalStaff;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    InspectionResult result;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "inspected_at", nullable = false)
    LocalDateTime inspectedAt;

    @Column(name = "registered_weight", nullable = false)
    Float registeredWeight;

    @Column(name = "actual_weight", nullable = false)
    Float actualWeight;

    @Column(name = "doping_detected", nullable = false)
    Boolean dopingDetected;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    InspectionStatus status;

}
