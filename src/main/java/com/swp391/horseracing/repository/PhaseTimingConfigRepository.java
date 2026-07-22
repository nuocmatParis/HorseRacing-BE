package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.PhaseTimingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PhaseTimingConfigRepository extends JpaRepository<PhaseTimingConfig, Long> {

    @Query("SELECT p FROM PhaseTimingConfig p WHERE p.phaseName = :phaseName "
            + "AND :capacity BETWEEN p.minCapacity AND p.maxCapacity")
    java.util.Optional<PhaseTimingConfig> findByPhaseNameAndCapacity(
            @Param("phaseName") String phaseName,
            @Param("capacity") int capacity);
}
