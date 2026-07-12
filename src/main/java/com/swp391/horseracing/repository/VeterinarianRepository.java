package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Veterinarian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VeterinarianRepository extends JpaRepository<Veterinarian, UUID> {

    Optional<Veterinarian> findByUser_UserId(UUID userId);

    boolean existsByUser_UserId(UUID userId);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndVetIdNot(String licenseNumber, UUID vetId);
}
