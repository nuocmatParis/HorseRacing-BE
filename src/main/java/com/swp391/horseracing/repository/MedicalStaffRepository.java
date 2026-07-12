package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.MedicalStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalStaffRepository extends JpaRepository<MedicalStaff, UUID> {

    Optional<MedicalStaff> findByUser_UserId(UUID userId);

    boolean existsByUser_UserId(UUID userId);
}
