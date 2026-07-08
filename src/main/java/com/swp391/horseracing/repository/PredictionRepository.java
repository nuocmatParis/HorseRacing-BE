package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Prediction;
import com.swp391.horseracing.enums.PredictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findByRace_RaceId(UUID raceId);

    List<Prediction> findBySpectator_SpectatorIdOrderByPredictionTimeDesc(UUID spectatorId);

    Optional<Prediction> findByRace_RaceIdAndSpectator_SpectatorId(UUID raceId, UUID spectatorId);

    boolean existsByRace_RaceIdAndSpectator_SpectatorIdAndStatusNot(UUID raceId, UUID spectatorId, PredictionStatus status);

    List<Prediction> findByRace_RaceIdAndStatus(UUID raceId, PredictionStatus status);
}
