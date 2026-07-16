package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.SpectatorHorseFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpectatorHorseFollowRepository extends JpaRepository<SpectatorHorseFollow, UUID> {
    boolean existsBySpectator_SpectatorIdAndHorse_HorseId(UUID spectatorId, UUID horseId);

    Optional<SpectatorHorseFollow> findBySpectator_SpectatorIdAndHorse_HorseId(UUID spectatorId, UUID horseId);

    Page<SpectatorHorseFollow> findBySpectator_SpectatorIdOrderByFollowedAtDesc(
            UUID spectatorId, Pageable pageable);
}
