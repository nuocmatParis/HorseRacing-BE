package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.RaceInspectionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RaceInspectionStaffAssignmentRepository
        extends JpaRepository<RaceInspectionAssignment, UUID> {

    Optional<RaceInspectionAssignment> findByRace_RaceId(UUID raceId);

    long countByVeterinarian_VetId(UUID veterinarianId);

    long countByMedicalStaff_MedStaffId(UUID medStaffId);
}