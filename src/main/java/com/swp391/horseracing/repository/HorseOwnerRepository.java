package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.HorseOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HorseOwnerRepository extends JpaRepository<HorseOwner, UUID> {
    Optional<HorseOwner> findByUser_UserId(UUID userId);
    Optional<HorseOwner> findByUser_Username(String username);
    boolean existsByUser_UserId(UUID userId);
}
