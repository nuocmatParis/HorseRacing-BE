package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.MedicalStaff;
import com.swp391.horseracing.enums.MedicalStaffStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalStaffRepository extends JpaRepository<MedicalStaff, UUID> {

    Optional<MedicalStaff> findByUser_UserId(UUID userId);

    List<MedicalStaff> findByStatus(MedicalStaffStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM MedicalStaff m
            WHERE m.medStaffId = :medStaffId
            """)
    Optional<MedicalStaff> findByIdForUpdate(@Param("medStaffId") UUID medStaffId);

    @Query("""
            SELECT m FROM MedicalStaff m
            LEFT JOIN RaceInspectionAssignment a ON a.medicalStaff = m
            WHERE m.status = :status
            GROUP BY m
            ORDER BY COUNT(a) ASC, m.yearsOfService DESC
            """)
    List<MedicalStaff> findBestAvailable(@Param("status") MedicalStaffStatus status, Pageable pageable);
}