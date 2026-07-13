package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.RaceInspectionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface RaceInspectionStaffAssignmentRepository
        extends JpaRepository<RaceInspectionAssignment, UUID> {

    Optional<RaceInspectionAssignment> findByRace_RaceId(UUID raceId);

    long countByVeterinarian_VetId(UUID veterinarianId);

    long countByMedicalStaff_MedStaffId(UUID medStaffId);

    List<RaceInspectionAssignment> findByVeterinarian_VetId(UUID vetId);

    List<RaceInspectionAssignment> findByMedicalStaff_MedStaffId(UUID medStaffId);

    @Query("""
            SELECT a FROM RaceInspectionAssignment a
            WHERE a.veterinarian.vetId = :vetId
              AND ((a.race.status = com.swp391.horseracing.enums.RoundStatus.SCHEDULED AND a.race.startTime >= :now)
                OR a.race.status = com.swp391.horseracing.enums.RoundStatus.ONGOING
                OR (a.race.status = com.swp391.horseracing.enums.RoundStatus.CANCELLED AND a.race.startTime >= :now))
            ORDER BY a.race.startTime ASC
            """)
    Page<RaceInspectionAssignment> findCurrentForVeterinarian(
            @Param("vetId") UUID vetId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            SELECT a FROM RaceInspectionAssignment a
            WHERE a.medicalStaff.medStaffId = :medicalStaffId
              AND ((a.race.status = com.swp391.horseracing.enums.RoundStatus.SCHEDULED AND a.race.startTime >= :now)
                OR a.race.status = com.swp391.horseracing.enums.RoundStatus.ONGOING
                OR (a.race.status = com.swp391.horseracing.enums.RoundStatus.CANCELLED AND a.race.startTime >= :now))
            ORDER BY a.race.startTime ASC
            """)
    Page<RaceInspectionAssignment> findCurrentForMedicalStaff(
            @Param("medicalStaffId") UUID medicalStaffId, @Param("now") LocalDateTime now, Pageable pageable);
}
