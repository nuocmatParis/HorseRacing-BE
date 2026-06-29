package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JockeyRepository extends JpaRepository<Jockey, UUID> {
    Optional<Jockey> findByUser_Username(String userName);
    boolean existsByUser_UserId(UUID userId);
    Optional<Jockey> findByUser_UserId(UUID userId);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberAndJockeyIdNot(String licenseNumber, UUID jockeyId);
}
