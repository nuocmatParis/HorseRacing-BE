package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.violation.request.ViolationCreateRequest;
import com.swp391.horseracing.dto.violation.response.ViolationResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.ViolationMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.UserCurrentService;
import com.swp391.horseracing.service.ViolationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ViolationServiceImpl implements ViolationService {

    RaceEntryRepository raceEntryRepository;
    RefereeRepository refereeRepository;
    RaceRefereeRepository raceRefereeRepository;
    ViolationRepository violationRepository;
    RaceReportRepository raceReportRepository;
    UserCurrentService userCurrentService;
    ViolationMapper violationMapper;

    @Override
    @Transactional
    public ViolationResponse createViolation(UUID entryId, ViolationCreateRequest request) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        Race race = raceEntry.getRace();
        RoundStatus raceStatus = race.getStatus();
        RaceReport report = raceReportRepository.findByRace_RaceId(race.getRaceId()).orElse(null);
        if (report != null && report.getStatus() != ReportStatus.DRAFT) {
            throw new AppException(ErrorCode.RACE_VIOLATION_REPORTING_CLOSED);
        }


        validateViolationType(raceStatus, request.getType());

        User currentUser = userCurrentService.getCurrentUser();
        Referee referee = refereeRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REFEREE_PROFILE_NOT_FOUND));

        if (referee.getStatus() == RefereeStatus.SUSPENDED) {
            throw new AppException(ErrorCode.REFEREE_NOT_AVAILABLE);
        }


        if (!raceRefereeRepository.existsByRace_RaceIdAndReferee_RefereeId(
                race.getRaceId(), referee.getRefereeId())) {
            throw new AppException(ErrorCode.REFEREE_NOT_ASSIGNED_TO_RACE);
        }

        LocalDateTime occurredAt = request.getOccurredAt();
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }

        Violation violation = Violation.builder()
                .raceEntry(raceEntry)
                .referee(referee)
                .type(request.getType())
                .description(request.getDescription())
                .penaltyType(request.getPenaltyType())
                .penaltyValue(null)
                .occurredAt(occurredAt)
                .createdAt(LocalDateTime.now())
                .status(ViolationStatus.ACTIVE)
                .build();

        if (request.getPenaltyType() == PenaltyType.DISQUALIFIED) {
            raceEntry.setStatus(RaceEntryStatus.DISQUALIFIED);
            raceEntry.setDisqualifiedAt(LocalDateTime.now());
            raceEntry.setDisqualifiedReason(request.getDescription());
            raceEntryRepository.save(raceEntry);
        }

        Violation savedViolation = violationRepository.save(violation);
        return violationMapper.toResponse(savedViolation);
    }

    @Override
    public List<ViolationResponse> getViolationsByRaceId(UUID raceId) {
        List<Violation> violations = violationRepository.findByRaceEntry_Race_RaceIdOrderByCreatedAtDesc(raceId);
        List<ViolationResponse> responseList = new ArrayList<>();
        for (Violation violation : violations) {
            responseList.add(violationMapper.toResponse(violation));
        }
        return responseList;
    }

    private void validateViolationType(RoundStatus raceStatus, ViolationType type) {
        if (raceStatus == RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.INVALID_VIOLATION_TYPE_FOR_RACE_STATUS);
        }
        if (raceStatus == RoundStatus.SCHEDULED) {
            if (type != ViolationType.FALSE_START && type != ViolationType.EQUIPMENT 
                    && type != ViolationType.DOPING && type != ViolationType.OTHER) {
                throw new AppException(ErrorCode.INVALID_VIOLATION_TYPE_FOR_RACE_STATUS);
            }
        }
        if (raceStatus == RoundStatus.FINISHED || raceStatus == RoundStatus.COMPLETED || raceStatus == RoundStatus.CANCELLED) {
            throw new AppException(ErrorCode.RACE_VIOLATION_REPORTING_CLOSED);
        }
    }
}
