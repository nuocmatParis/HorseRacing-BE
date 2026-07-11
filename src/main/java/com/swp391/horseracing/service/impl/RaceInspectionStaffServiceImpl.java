package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.medicalstaff.request.AssignInspectionStaffRequest;
import com.swp391.horseracing.dto.medicalstaff.response.InspectionStaffAssignmentResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.MedicalStaffStatus;
import com.swp391.horseracing.enums.VetStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceInspectionAssignmentMapper;
import com.swp391.horseracing.repository.MedicalStaffRepository;
import com.swp391.horseracing.repository.RaceInspectionStaffAssignmentRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.VeterinarianRepository;
import com.swp391.horseracing.service.RaceInspectionStaffService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RaceInspectionStaffServiceImpl implements RaceInspectionStaffService {
    RaceRepository raceRepository;
    MedicalStaffRepository medicalStaffRepository;
    RaceInspectionStaffAssignmentRepository raceInspectionStaffAssignmentRepository;
    VeterinarianRepository veterinarianRepository;
    UserCurrentService userCurrentService;
    RaceInspectionAssignmentMapper raceInspectionAssignmentMapper;


    @Override
    @Transactional
    public InspectionStaffAssignmentResponse assign(UUID raceId, AssignInspectionStaffRequest request) {
        Race race = raceRepository.findById(raceId).orElseThrow(()
                -> new AppException(ErrorCode.RACE_NOT_FOUND));

        MedicalStaff medicalStaff = medicalStaffRepository.findByIdForUpdate(request.getMedStaffId()).orElseThrow(()
                -> new AppException(ErrorCode.MEDICAL_STAFF_NOT_FOUND));

        if(medicalStaff.getStatus() == MedicalStaffStatus.SUSPENDED)
            throw new AppException(ErrorCode.MEDICAL_STAFF_SUSPENDED);

        if(medicalStaff.getStatus() == MedicalStaffStatus.ASSIGNED)
            throw new AppException(ErrorCode.MEDICAL_STAFF_ALREADY_ASSIGNED);

        Veterinarian veterinarian = veterinarianRepository.findByIdForUpdate(request.getVeterinarianId()).orElseThrow(()
                -> new AppException(ErrorCode.VETERINARIAN_NOT_FOUND));

        if(veterinarian.getStatus() == VetStatus.SUSPENDED)
            throw new AppException(ErrorCode.VETERINARIAN_SUSPENDED);

        if(veterinarian.getStatus() == VetStatus.ASSIGNED)
            throw new AppException(ErrorCode.VETERINARIAN_ALREADY_ASSIGNED);

        Optional<RaceInspectionAssignment> optional = raceInspectionStaffAssignmentRepository.findByRace_RaceId(raceId);

        RaceInspectionAssignment assignment;

        if (optional.isPresent())
            assignment = optional.get();
        else
            assignment = new RaceInspectionAssignment();

        if(assignment.getAssignmentId() != null && assignment.getMedicalStaff() != null
                && !assignment.getMedicalStaff().getMedStaffId().equals(medicalStaff.getMedStaffId())){
            MedicalStaff oldMedicalStaff = assignment.getMedicalStaff();

            oldMedicalStaff.setStatus(MedicalStaffStatus.AVAILABLE);

            medicalStaffRepository.save(oldMedicalStaff);
        }

        if(assignment.getAssignmentId() != null && assignment.getVeterinarian() != null
                && !assignment.getVeterinarian().getVetId().equals(veterinarian.getVetId())){
            Veterinarian oldVet = assignment.getVeterinarian();

            oldVet.setStatus(VetStatus.AVAILABLE);

            veterinarianRepository.save(oldVet);
        }

        User admin = userCurrentService.getCurrentUser();
        assignment.setRace(race);
        assignment.setAssignedBy(admin);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setMedicalStaff(medicalStaff);
        assignment.setVeterinarian(veterinarian);

        medicalStaff.setStatus(MedicalStaffStatus.ASSIGNED);
        medicalStaffRepository.save(medicalStaff);

        veterinarian.setStatus(VetStatus.ASSIGNED);
        veterinarianRepository.save(veterinarian);

        return raceInspectionAssignmentMapper.toInspectionStaffAssignmentResponse(raceInspectionStaffAssignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public InspectionStaffAssignmentResponse autoAssign(UUID raceId) {
        Race race = raceRepository.findById(raceId).orElseThrow(()
                -> new AppException(ErrorCode.RACE_NOT_FOUND));

        List<MedicalStaff> medicalStaffCandidates = medicalStaffRepository
                .findBestAvailable(MedicalStaffStatus.AVAILABLE, PageRequest.of(0, 1));

        if (medicalStaffCandidates.isEmpty()) {
            throw new AppException(ErrorCode.NO_AVAILABLE_MEDICAL_STAFF);
        }

        MedicalStaff medicalStaff = medicalStaffCandidates.get(0);

        List<Veterinarian> veterinarianCandidates = veterinarianRepository
                .findBestAvailable(VetStatus.AVAILABLE, PageRequest.of(0, 1));

        if (veterinarianCandidates.isEmpty()) {
            throw new AppException(ErrorCode.NO_AVAILABLE_VETERINARIAN);
        }

        Veterinarian veterinarian = veterinarianCandidates.get(0);

        Optional<RaceInspectionAssignment> optional = raceInspectionStaffAssignmentRepository.findByRace_RaceId(raceId);

        RaceInspectionAssignment assignment;

        if (optional.isPresent())
            assignment = optional.get();
        else
            assignment = new RaceInspectionAssignment();

        if (assignment.getAssignmentId() != null && assignment.getMedicalStaff() != null
                && !assignment.getMedicalStaff().getMedStaffId().equals(medicalStaff.getMedStaffId())) {
            MedicalStaff oldMedicalStaff = assignment.getMedicalStaff();

            oldMedicalStaff.setStatus(MedicalStaffStatus.AVAILABLE);

            medicalStaffRepository.save(oldMedicalStaff);
        }

        if (assignment.getAssignmentId() != null && assignment.getVeterinarian() != null
                && !assignment.getVeterinarian().getVetId().equals(veterinarian.getVetId())) {
            Veterinarian oldVet = assignment.getVeterinarian();

            oldVet.setStatus(VetStatus.AVAILABLE);

            veterinarianRepository.save(oldVet);
        }

        User admin = userCurrentService.getCurrentUser();
        assignment.setRace(race);
        assignment.setAssignedBy(admin);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setMedicalStaff(medicalStaff);
        assignment.setVeterinarian(veterinarian);

        medicalStaff.setStatus(MedicalStaffStatus.ASSIGNED);
        medicalStaffRepository.save(medicalStaff);

        veterinarian.setStatus(VetStatus.ASSIGNED);
        veterinarianRepository.save(veterinarian);

        return raceInspectionAssignmentMapper.toInspectionStaffAssignmentResponse(raceInspectionStaffAssignmentRepository.save(assignment));
    }
}
