package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Appeal;
import com.swp391.horseracing.enums.AppealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, UUID> {

    List<Appeal> findByEntry_EntryIdAndStatus(UUID entryId, AppealStatus status);

    List<Appeal> findBySubmittedBy_UserId(UUID userId);

    List<Appeal> findByEntry_Race_RaceId(UUID raceId);

    boolean existsByEntry_Race_RaceIdAndStatus(UUID raceId, AppealStatus status);

    List<Appeal> findByEntry_Race_RaceIdOrderBySubmittedAtDesc(UUID raceId);
}
