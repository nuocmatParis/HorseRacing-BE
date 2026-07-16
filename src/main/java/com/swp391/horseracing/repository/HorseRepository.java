package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Horse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HorseRepository extends JpaRepository<Horse, UUID> {
    List<Horse> findByOwner_OwnerId(UUID ownerId);
    Optional<Horse> findByHorseIdAndOwner_OwnerId(UUID horseId, UUID ownerId);
    Optional<Horse> findByName(String name);

    @Query("SELECT h FROM Horse h WHERE "
            + "(:query IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(CAST(h.breed AS string)) LIKE LOWER(CONCAT('%', :query, '%'))) "
            + "AND (:raceClass IS NULL OR CAST(h.raceClass AS string) = :raceClass) "
            + "AND (:healthStatus IS NULL OR CAST(h.healthStatus AS string) = :healthStatus) "
            + "ORDER BY h.name ASC")
    Page<Horse> searchForSpectator(
            @Param("query") String query,
            @Param("raceClass") String raceClass,
            @Param("healthStatus") String healthStatus,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Horse h WHERE h.horseId IN :horseIds ORDER BY h.horseId")
    List<Horse> findAllForUpdateByHorseIdIn(
            @Param("horseIds") Collection<UUID> horseIds
    );
}
