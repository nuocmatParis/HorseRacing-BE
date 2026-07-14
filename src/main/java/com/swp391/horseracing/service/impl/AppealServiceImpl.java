package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.appeal.request.CreateAppealRequest;
import com.swp391.horseracing.dto.appeal.request.ReviewAppealRequest;
import com.swp391.horseracing.dto.appeal.response.AppealResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.AppealStatus;
import com.swp391.horseracing.enums.RefereeStatus;
import com.swp391.horseracing.enums.ReportStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AppealMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.AppealService;
import com.swp391.horseracing.service.UserCurrentService;
import com.swp391.horseracing.service.BusinessNotificationEventService;
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
public class AppealServiceImpl implements AppealService {

    AppealRepository appealRepository;
    AppealCategoryRepository appealCategoryRepository;
    RaceEntryRepository raceEntryRepository;
    RaceResultRepository raceResultRepository;
    RaceReportRepository raceReportRepository;
    HorseOwnerRepository horseOwnerRepository;
    JockeyRepository jockeyRepository;
    RefereeRepository refereeRepository;
    RaceRefereeRepository raceRefereeRepository;
    AppealMapper appealMapper;
    ViolationRepository violationRepository;
    UserCurrentService userCurrentService;
    BusinessNotificationEventService notificationEventService;

    @Override
    @Transactional
    public AppealResponse create(CreateAppealRequest request) {
        User currentUser = userCurrentService.getCurrentUser();

        if (currentUser.getStatus() != com.swp391.horseracing.enums.AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        RaceEntry entry = raceEntryRepository.findById(request.getEntryId())
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        boolean isOwner = horseOwnerRepository.findByUser_UserId(currentUser.getUserId())
                .map(owner -> owner.getOwnerId().equals(entry.getContract().getOwner().getOwnerId()))
                .orElse(false);

        boolean isJockey = jockeyRepository.findByUser_UserId(currentUser.getUserId())
                .map(jockey -> jockey.getJockeyId().equals(entry.getContract().getJockey().getJockeyId()))
                .orElse(false);

        if (!isOwner && !isJockey) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Race race = entry.getRace();
        if (race.getStartedAt() == null || race.getStatus() == RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.RACE_HAS_NOT_STARTED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(race.getEndTime())) {
            throw new AppException(ErrorCode.APPEAL_SUBMISSION_CLOSED);
        }

        if (raceReportRepository.existsByRace_RaceId(entry.getRace().getRaceId())) {
            RaceReport report = raceReportRepository.findByRace_RaceId(entry.getRace().getRaceId()).get();
            if (report.getStatus() == ReportStatus.Published) {
                throw new AppException(ErrorCode.RACE_REPORT_ALREADY_PUBLISHED);
            }
        }

        if (!appealCategoryRepository.existsById(request.getCategoryId())) {
            throw new AppException(ErrorCode.APPEAL_CATEGORY_NOT_FOUND);
        }

        List<Appeal> pendingAppeals = appealRepository.findByEntry_EntryIdAndStatus(
                request.getEntryId(), AppealStatus.Pending);
        if (!pendingAppeals.isEmpty()) {
            throw new AppException(ErrorCode.APPEAL_ALREADY_REVIEWED);
        }

        AppealCategory category = appealCategoryRepository.findById(request.getCategoryId()).get();
        if (!category.getIsActive()) {
            throw new AppException(ErrorCode.APPEAL_CATEGORY_INACTIVE);
        }

        Appeal appeal = Appeal.builder()
                .entry(entry)
                .category(category)
                .submittedBy(currentUser)
                .description(request.getDescription())
                .status(AppealStatus.Pending)
                .build();

        if (request.getRaceResultId() != null) {
            RaceResult raceResult = raceResultRepository.findById(request.getRaceResultId())
                    .orElseThrow(() -> new AppException(ErrorCode.RACE_RESULT_NOT_FOUND));
            appeal.setRaceResult(raceResult);
        }

        if (request.getRelatedViolationId() != null) {
            Violation violation = violationRepository.findById(request.getRelatedViolationId())
                    .orElseThrow(() -> new AppException(ErrorCode.VIOLATION_NOT_FOUND));
            appeal.setRelatedViolation(violation);
        }

        Appeal savedAppeal = appealRepository.save(appeal);
        notificationEventService.appealSubmitted(savedAppeal);
        return appealMapper.toAppealResponse(savedAppeal);
    }

    @Override
    @Transactional
    public AppealResponse update(UUID appealId, CreateAppealRequest request) {
        User currentUser = userCurrentService.getCurrentUser();

        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));

        if (appeal.getStatus() != AppealStatus.Pending) {
            throw new AppException(ErrorCode.APPEAL_NOT_PENDING);
        }

        if (!appeal.getSubmittedBy().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (request.getCategoryId() != null) {
            AppealCategory category = appealCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.APPEAL_CATEGORY_NOT_FOUND));
            if (!category.getIsActive()) {
                throw new AppException(ErrorCode.APPEAL_CATEGORY_INACTIVE);
            }
            appeal.setCategory(category);
        }

        if (request.getDescription() != null) {
            appeal.setDescription(request.getDescription());
        }

        return appealMapper.toAppealResponse(appealRepository.save(appeal));
    }

    @Override
    @Transactional
    public void cancel(UUID appealId) {
        User currentUser = userCurrentService.getCurrentUser();

        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));

        if (appeal.getStatus() != AppealStatus.Pending) {
            throw new AppException(ErrorCode.APPEAL_NOT_PENDING);
        }

        if (!appeal.getSubmittedBy().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        appeal.setStatus(AppealStatus.Cancelled);
        appealRepository.save(appeal);
    }

    @Override
    public List<AppealResponse> getMyAppeals() {
        User currentUser = userCurrentService.getCurrentUser();
        return appealRepository.findBySubmittedBy_UserId(currentUser.getUserId())
                .stream()
                .map(appealMapper::toAppealResponse)
                .toList();
    }

    @Override
    public List<AppealResponse> getAllAppeals() {
        return appealRepository.findAll()
                .stream()
                .map(appealMapper::toAppealResponse)
                .toList();
    }

    @Override
    public AppealResponse getAppealDetail(UUID appealId) {
        return appealMapper.toAppealResponse(
                appealRepository.findById(appealId)
                        .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND)));
    }

    @Override
    @Transactional
    public AppealResponse review(UUID appealId, ReviewAppealRequest request) {
        User currentUser = userCurrentService.getCurrentUser();

        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (referee.getStatus() == RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new AppException(ErrorCode.APPEAL_NOT_FOUND));

        RaceEntry entry = appeal.getEntry();
        Race race = entry.getRace();
        Round round = race.getRound();
        if (round.getHeadReferee() == null || !round.getHeadReferee().getRefereeId().equals(referee.getRefereeId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (appeal.getStatus() != AppealStatus.Pending) {
            throw new AppException(ErrorCode.APPEAL_NOT_PENDING);
        }

        if (request.getStatus() != AppealStatus.Accepted && request.getStatus() != AppealStatus.Rejected) {
            throw new AppException(ErrorCode.INVALID_APPEAL_STATUS_TRANSITION);
        }

        appeal.setStatus(request.getStatus());
        appeal.setResolution(request.getResolution());
        appeal.setReviewedBy(referee);
        appeal.setReviewedAt(LocalDateTime.now());

        Appeal savedAppeal = appealRepository.save(appeal);
        notificationEventService.appealReviewed(savedAppeal);
        return appealMapper.toAppealResponse(savedAppeal);
    }
}
