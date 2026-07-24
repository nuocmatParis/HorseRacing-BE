package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.medicalstaff.request.AssignInspectionStaffRequest;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceInspectionAssignmentMapper;
import com.swp391.horseracing.repository.HorseInspectionRepository;
import com.swp391.horseracing.repository.JockeyInspectionRepository;
import com.swp391.horseracing.repository.MedicalStaffRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceInspectionStaffAssignmentRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.VeterinarianRepository;
import com.swp391.horseracing.service.impl.RaceInspectionStaffServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceInspectionStaffAssignmentGuardTest {

    @Mock RaceRepository raceRepository;
    @Mock MedicalStaffRepository medicalStaffRepository;
    @Mock RaceInspectionStaffAssignmentRepository assignmentRepository;
    @Mock VeterinarianRepository veterinarianRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock HorseInspectionRepository horseInspectionRepository;
    @Mock JockeyInspectionRepository jockeyInspectionRepository;
    @Mock UserCurrentService userCurrentService;
    @Mock RaceInspectionAssignmentMapper assignmentMapper;

    @InjectMocks RaceInspectionStaffServiceImpl service;

    @Test
    void cannotAssignInspectionStaffAfterRaceStarts() {
        UUID raceId = UUID.randomUUID();
        Race race = Race.builder().raceId(raceId).status(RoundStatus.ONGOING).build();
        AssignInspectionStaffRequest request = new AssignInspectionStaffRequest();
        request.setMedStaffId(UUID.randomUUID());
        request.setVeterinarianId(UUID.randomUUID());
        when(raceRepository.findForUpdateByRaceId(raceId)).thenReturn(Optional.of(race));

        AppException exception = assertThrows(AppException.class,
                () -> service.assign(raceId, request));

        assertEquals(ErrorCode.INSPECTION_STAFF_ASSIGNMENT_NOT_ALLOWED,
                exception.getErrorCode());
        verify(medicalStaffRepository, never()).findByIdForUpdate(request.getMedStaffId());
        verify(veterinarianRepository, never()).findByIdForUpdate(request.getVeterinarianId());
    }
}
