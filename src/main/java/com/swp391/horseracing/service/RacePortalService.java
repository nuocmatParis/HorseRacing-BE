package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.race_portal.AssignedRaceResponse;
import com.swp391.horseracing.dto.race_portal.RaceResultsResponse;
import com.swp391.horseracing.dto.race_portal.RaceScheduleResponse;
import com.swp391.horseracing.dto.race_portal.RaceSummaryResponse;
import com.swp391.horseracing.dto.race_portal.SpectatorRaceDetailResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RacePortalService {
    PageResponse<RaceScheduleResponse> getOwnerSchedule(int page, int size);
    PageResponse<RaceScheduleResponse> getJockeySchedule(int page, int size);
    PageResponse<RaceResultsResponse> getOwnerResults(int page, int size);
    PageResponse<RaceResultsResponse> getJockeyResults(int page, int size);
    PageResponse<RaceSummaryResponse> getUpcomingRaces(
            LocalDateTime from, LocalDateTime to, UUID tournamentId, int page, int size);
    SpectatorRaceDetailResponse getSpectatorRaceDetail(UUID raceId);
    PageResponse<AssignedRaceResponse> getRefereeAssignedRaces(int page, int size);
    PageResponse<AssignedRaceResponse> getVeterinarianAssignedRaces(int page, int size);
    PageResponse<AssignedRaceResponse> getMedicalAssignedRaces(int page, int size);
}
