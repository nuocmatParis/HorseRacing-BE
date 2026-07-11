package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Veterinarian;
import com.swp391.horseracing.enums.VetStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeterinarianRepository extends JpaRepository<Veterinarian, UUID> {

    Optional<Veterinarian> findByUser_UserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT v
            FROM Veterinarian v 
            WHERE v.vetId = :vetId
            """)
    Optional<Veterinarian> findByIdForUpdate(@Param("vetId") UUID vetId);

    @Query("""
            SELECT v FROM Veterinarian v
            LEFT JOIN RaceInspectionAssignment a ON a.veterinarian = v
            WHERE v.status = :status
            GROUP BY v
            ORDER BY COUNT(a) ASC, v.yearsOfService DESC
            """)
    List<Veterinarian> findBestAvailable(@Param("status") VetStatus status, Pageable pageable);
}
