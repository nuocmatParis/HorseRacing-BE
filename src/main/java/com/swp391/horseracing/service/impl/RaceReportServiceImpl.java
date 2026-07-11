package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.AppealStatus;
import com.swp391.horseracing.enums.NotificationType;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceReportMapper;
import com.swp391.horseracing.mapper.RaceResultMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.NotificationService;
import com.swp391.horseracing.service.RaceReportService;
import com.swp391.horseracing.service.ScoringService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceReportServiceImpl implements RaceReportService {

    RaceReportRepository raceReportRepository;
    RaceRepository raceRepository;
    RaceResultRepository raceResultRepository;
    RaceEntryRepository raceEntryRepository;
    RaceRefereeRepository raceRefereeRepository;
    RefereeRepository refereeRepository;
    AppealRepository appealRepository;
    RaceReportMapper raceReportMapper;
    RaceResultMapper raceResultMapper;
    UserCurrentService userCurrentService;
    NotificationService notificationService;
    ScoringService scoringService;

    @Override
    @Transactional
    public RaceReportResponse getRefereeReport(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        User currentUser = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (!raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(raceId, referee.getRefereeId())) {
            throw new AppException(ErrorCode.RACE_REFEREE_NOT_FOUND);
        }

        if (!raceResultRepository.existsByRace_RaceId(raceId)) {
            throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
        }

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseGet(() -> {
                    RaceReport newReport = RaceReport.builder()
                            .race(race)
                            .referee(referee)
                            .summary("")
                            .status(ReportStatus.Draft)
                            .build();
                    return raceReportRepository.save(newReport);
                });

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    @Transactional
    public RaceReportResponse signReport(UUID raceId, UUID refereeId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.FINISHED && race.getStatus() != RoundStatus.ONGOING) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        User currentUser = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        Round round = race.getRound();
        if (round.getHeadReferee() == null || !round.getHeadReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (!raceResultRepository.existsByRace_RaceId(raceId)) {
            throw new AppException(ErrorCode.RACE_RESULT_NOT_FOUND);
        }

        boolean hasPendingAppeals = appealRepository.existsByEntry_Race_RaceIdAndStatus(
                raceId, AppealStatus.Pending);
        if (hasPendingAppeals) {
            throw new AppException(ErrorCode.APPEAL_NOT_PENDING);
        }

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Draft) {
            throw new AppException(ErrorCode.RACE_REPORT_ALREADY_SIGNED);
        }

        report.setStatus(ReportStatus.Signed);
        report.setSignedBy(referee);
        report.setSignedAt(LocalDateTime.now());

        return raceReportMapper.toRaceReportResponse(raceReportRepository.save(report));
    }

    @Override
    @Transactional
    public RaceReportResponse getAdminReport(UUID raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    @Transactional
    public RaceReportResponse publishReport(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.FINISHED) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Signed) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_SIGNED);
        }

        User currentUser = userCurrentService.getCurrentUser();

        report.setStatus(ReportStatus.Published);
        report.setPublishedBy(currentUser);
        report.setPublishedAt(LocalDateTime.now());
        raceReportRepository.save(report);

        race.setStatus(RoundStatus.COMPLETED);
        raceRepository.save(race);

        scoringService.scoreRace(raceId);

        sendNotificationsForPublishedReport(race);

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    public RaceReportResponse getPublishedReport(UUID raceId) {
        RaceReport report = raceReportRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.Published) {
            throw new AppException(ErrorCode.RACE_REPORT_NOT_FOUND);
        }

        return raceReportMapper.toRaceReportResponse(report);
    }

    @Override
    public List<RaceResultResponse> getRaceRanking(UUID raceId) {
        raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        return raceResultRepository.findByRace_RaceIdOrderByRankAsc(raceId)
                .stream()
                .map(raceResultMapper::toRaceResultResponse)
                .toList();
    }

    private void sendNotificationsForPublishedReport(Race race) {
        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(
                race.getRaceId());

        for (RaceEntry entry : entries) {
            UUID ownerUserId = entry.getContract().getOwner().getUser().getUserId();
            UUID jockeyUserId = entry.getContract().getJockey().getUser().getUserId();

            notificationService.sendNotification(
                    ownerUserId,
                    "Race Result Published",
                    "Race \"" + race.getName() + "\" results have been published.",
                    NotificationType.ResultPublished,
                    "Race",
                    race.getRaceId()
            );

            notificationService.sendNotification(
                    jockeyUserId,
                    "Race Result Published",
                    "Race \"" + race.getName() + "\" results have been published.",
                    NotificationType.ResultPublished,
                    "Race",
                    race.getRaceId()
            );
        }
    }
}
