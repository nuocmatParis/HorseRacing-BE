package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.jockeyinspection.request.JockeyInspectionRequest;
import com.swp391.horseracing.dto.jockeyinspection.response.JockeyInspectionResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.JockeyInspectionMapper;
import com.swp391.horseracing.repository.JockeyInspectionRepository;
import com.swp391.horseracing.repository.MedicalStaffRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceInspectionStaffAssignmentRepository;
import com.swp391.horseracing.service.JockeyInspectionService;
import com.swp391.horseracing.service.UserCurrentService;
import com.swp391.horseracing.service.PredictionService;
import com.swp391.horseracing.service.BusinessNotificationEventService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JockeyInspectionServiceImpl implements JockeyInspectionService {

    RaceEntryRepository raceEntryRepository;
    JockeyInspectionRepository jockeyInspectionRepository;
    MedicalStaffRepository medicalStaffRepository;
    RaceInspectionStaffAssignmentRepository raceInspectionStaffAssignmentRepository;
    UserCurrentService userCurrentService;
    JockeyInspectionMapper jockeyInspectionMapper;
    PredictionService predictionService;
    BusinessNotificationEventService notificationEventService;

    @Override
    @Transactional(readOnly = true)
    public JockeyInspectionResponse getInspection(UUID entryId) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
        User currentUser = userCurrentService.getCurrentUser();
        MedicalStaff medicalStaff = medicalStaffRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_PROFILE_NOT_FOUND));
        RaceInspectionAssignment assignment = raceInspectionStaffAssignmentRepository
                .findByRace_RaceId(raceEntry.getRace().getRaceId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_NOT_ASSIGNED_TO_RACE));
        if (!assignment.getMedicalStaff().getMedStaffId().equals(medicalStaff.getMedStaffId())) {
            throw new AppException(ErrorCode.MEDICAL_STAFF_NOT_ASSIGNED_TO_RACE);
        }
        JockeyInspection inspection = jockeyInspectionRepository.findByRaceEntry_EntryId(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_INSPECTION_NOT_FOUND));
        return jockeyInspectionMapper.toResponse(inspection);
    }

    @Override
    @Transactional
    public JockeyInspectionResponse createInspection(UUID entryId, JockeyInspectionRequest request) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        Race race = raceEntry.getRace();
        if (race.getStatus() != RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULED_STATUS);
        }

        if (raceEntry.getStatus() != RaceEntryStatus.CONFIRMED) {
            throw new AppException(ErrorCode.RACE_ENTRY_NOT_ACTIVE);
        }

        if (race.getStartedAt() != null) {
            throw new AppException(ErrorCode.INSPECTION_WINDOW_CLOSED);
        }

        Tournament tournament = race.getRound().getTournament();
        int openMin = tournament.getInspectionOpenMinutesBefore();
        int closeMin = tournament.getInspectionCloseMinutesBefore();
        LocalDateTime inspectionOpenAt = race.getStartTime().minusMinutes(openMin);
        LocalDateTime inspectionCloseAt = race.getStartTime().minusMinutes(closeMin);
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(inspectionOpenAt)) {
            throw new AppException(ErrorCode.INSPECTION_WINDOW_NOT_OPEN);
        }
        if (now.isAfter(inspectionCloseAt)) {
            throw new AppException(ErrorCode.INSPECTION_WINDOW_CLOSED);
        }

        if (jockeyInspectionRepository.existsByRaceEntry_EntryId(entryId)) {
            throw new AppException(ErrorCode.JOCKEY_INSPECTION_ALREADY_EXISTS);
        }

        User currentUser = userCurrentService.getCurrentUser();
        MedicalStaff medStaff = medicalStaffRepository.findByUser_UserId(currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_PROFILE_NOT_FOUND));

        RaceInspectionAssignment assignment = raceInspectionStaffAssignmentRepository
                .findByRace_RaceId(race.getRaceId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_NOT_ASSIGNED_TO_RACE));

        if (!assignment.getMedicalStaff().getMedStaffId().equals(medStaff.getMedStaffId())) {
            throw new AppException(ErrorCode.MEDICAL_STAFF_NOT_ASSIGNED_TO_RACE);
        }

        JockeyInspection inspection = JockeyInspection.builder()
                .raceEntry(raceEntry)
                .medicalStaff(medStaff)
                .result(request.getResult())
                .note(request.getNote())
                .inspectedAt(LocalDateTime.now())
                .status(InspectionStatus.CONFIRMED)
                .build();

        if (request.getResult() == InspectionResult.FAIL) {
            raceEntry.setStatus(RaceEntryStatus.SCRATCHED);
            raceEntry.setScratchedReason("Failed jockey inspection. Note: " + request.getNote());
            raceEntryRepository.save(raceEntry);
            predictionService.notifySpectatorsForScratchedEntry(
                    race.getRaceId(), entryId, raceEntry.getContract().getHorse().getName());
        }

        JockeyInspection savedInspection = jockeyInspectionRepository.save(inspection);
        if (request.getResult() == InspectionResult.FAIL) {
            notificationEventService.jockeyInspectionFailed(raceEntry);
        }
        return jockeyInspectionMapper.toResponse(savedInspection);
    }
}
