package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.RaceReferee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RaceRefereeRepository extends JpaRepository<RaceReferee, UUID> {

    List<RaceReferee> findByRace_RaceId(UUID raceId);

    boolean existsByRace_RaceIdAndReferee_RefereeId(UUID raceId, UUID refereeId);

    java.util.Optional<RaceReferee> findByRace_RaceIdAndReferee_RefereeId(UUID raceId, UUID refereeId);

    int countByRace_RaceId(UUID raceId);

    boolean existsByReferee_RefereeId(UUID refereeId);

    List<RaceReferee> findByReferee_RefereeId(UUID refereeId);
}
