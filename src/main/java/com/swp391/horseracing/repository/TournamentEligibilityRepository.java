package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.TournamentEligibility;
import org.springframework.data.jpa.repository.JpaRepository;

import com.swp391.horseracing.enums.EligibilityCondition;
import com.swp391.horseracing.enums.EligibilityTargetType;
import java.util.List;
import java.util.UUID;

public interface TournamentEligibilityRepository extends JpaRepository<TournamentEligibility, UUID> {
    List<TournamentEligibility> findByTournament_TournamentId(UUID tournamentId);
    boolean existsByTournament_TournamentIdAndTargetTypeAndConditionName(
            UUID tournamentId, EligibilityTargetType targetType, EligibilityCondition conditionName);
    boolean existsByTournament_TournamentIdAndTargetTypeAndConditionNameAndEligibilityIdNot(
            UUID tournamentId, EligibilityTargetType targetType,
            EligibilityCondition conditionName, UUID eligibilityId);
}
