package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;

import java.util.List;
import java.util.UUID;

public interface RaceReportService {

    RaceReportResponse getRefereeReport(UUID raceId);

    RaceReportResponse signReport(UUID raceId, UUID refereeId);

    RaceReportResponse getAdminReport(UUID raceId);

    RaceReportResponse publishReport(UUID raceId);

    RaceReportResponse getPublishedReport(UUID raceId);

    List<RaceResultResponse> getRaceRanking(UUID raceId);
}
