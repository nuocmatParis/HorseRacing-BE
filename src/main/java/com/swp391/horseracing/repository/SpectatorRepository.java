package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Spectator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpectatorRepository extends JpaRepository<Spectator, UUID> {

    Optional<Spectator> findByUser_UserId(UUID userId);
}
