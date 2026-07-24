package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.medicalstaff.request.AssignInspectionStaffRequest;
import com.swp391.horseracing.dto.medicalstaff.response.InspectionStaffAssignmentResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.MedicalStaffStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.VetStatus;
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
import com.swp391.horseracing.service.RaceInspectionStaffService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RaceInspectionStaffServiceImpl implements RaceInspectionStaffService {
    RaceRepository raceRepository;
    MedicalStaffRepository medicalStaffRepository;
    RaceInspectionStaffAssignmentRepository raceInspectionStaffAssignmentRepository;
    VeterinarianRepository veterinarianRepository;
    RaceEntryRepository raceEntryRepository;
    HorseInspectionRepository horseInspectionRepository;
    JockeyInspectionRepository jockeyInspectionRepository;
    UserCurrentService userCurrentService;
    RaceInspectionAssignmentMapper raceInspectionAssignmentMapper;


    @Override
    @Transactional
    public InspectionStaffAssignmentResponse assign(UUID raceId, AssignInspectionStaffRequest request) {
        Race race = raceRepository.findForUpdateByRaceId(raceId).orElseThrow(()
                -> new AppException(ErrorCode.RACE_NOT_FOUND));
        validateRaceCanAssignInspectionStaff(race);

        Optional<RaceInspectionAssignment> optional =
                raceInspectionStaffAssignmentRepository.findByRace_RaceId(raceId);
        RaceInspectionAssignment assignment = optional.orElseGet(RaceInspectionAssignment::new);

        MedicalStaff medicalStaff = medicalStaffRepository.findByIdForUpdate(request.getMedStaffId()).orElseThrow(()
                -> new AppException(ErrorCode.MEDICAL_STAFF_NOT_FOUND));

        if(medicalStaff.getStatus() == MedicalStaffStatus.SUSPENDED)
            throw new AppException(ErrorCode.MEDICAL_STAFF_SUSPENDED);

        Veterinarian veterinarian = veterinarianRepository.findByIdForUpdate(request.getVeterinarianId()).orElseThrow(()
                -> new AppException(ErrorCode.VETERINARIAN_NOT_FOUND));

        if(veterinarian.getStatus() == VetStatus.SUSPENDED)
            throw new AppException(ErrorCode.VETERINARIAN_SUSPENDED);

        if (isSameAssignment(assignment, request.getMedStaffId(), request.getVeterinarianId())) {
            return raceInspectionAssignmentMapper.toInspectionStaffAssignmentResponse(assignment);
        }

        if(medicalStaff.getStatus() == MedicalStaffStatus.ASSIGNED)
            throw new AppException(ErrorCode.MEDICAL_STAFF_ALREADY_ASSIGNED);

        if(veterinarian.getStatus() == VetStatus.ASSIGNED)
            throw new AppException(ErrorCode.VETERINARIAN_ALREADY_ASSIGNED);

        if(assignment.getMedicalStaff() != null
                && !assignment.getMedicalStaff().getMedStaffId().equals(medicalStaff.getMedStaffId())){
            MedicalStaff oldMedicalStaff = assignment.getMedicalStaff();

            oldMedicalStaff.setStatus(MedicalStaffStatus.AVAILABLE);

            medicalStaffRepository.save(oldMedicalStaff);
        }

        if(assignment.getVeterinarian() != null
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
        Race race = raceRepository.findForUpdateByRaceId(raceId).orElseThrow(()
                -> new AppException(ErrorCode.RACE_NOT_FOUND));
        validateRaceCanAssignInspectionStaff(race);

        Optional<RaceInspectionAssignment> optional = raceInspectionStaffAssignmentRepository.findByRace_RaceId(raceId);
        if (optional.isPresent() && optional.get().getMedicalStaff() != null && optional.get().getVeterinarian() != null) {
            return raceInspectionAssignmentMapper.toInspectionStaffAssignmentResponse(optional.get());
        }

        MedicalStaff medicalStaff = lockAvailableMedicalStaff();
        Veterinarian veterinarian = lockAvailableVeterinarian();
        RaceInspectionAssignment assignment = optional.orElseGet(RaceInspectionAssignment::new);

        if (assignment.getMedicalStaff() != null
                && !assignment.getMedicalStaff().getMedStaffId().equals(medicalStaff.getMedStaffId())) {
            MedicalStaff oldMedicalStaff = assignment.getMedicalStaff();

            oldMedicalStaff.setStatus(MedicalStaffStatus.AVAILABLE);

            medicalStaffRepository.save(oldMedicalStaff);
        }

        if (assignment.getVeterinarian() != null && !assignment.getVeterinarian().getVetId().equals(veterinarian.getVetId())) {
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

    private void validateRaceCanAssignInspectionStaff(Race race) {
        if (race.getStatus() != RoundStatus.SCHEDULING
                && race.getStatus() != RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.INSPECTION_STAFF_ASSIGNMENT_NOT_ALLOWED);
        }

        List<RaceEntry> entries =
                raceEntryRepository.findByRace_RaceIdOrderByCreatedAtAsc(race.getRaceId());
        for (RaceEntry entry : entries) {
            if (horseInspectionRepository.existsByRaceEntry_EntryId(entry.getEntryId())
                    || jockeyInspectionRepository.existsByRaceEntry_EntryId(entry.getEntryId())) {
                throw new AppException(ErrorCode.INSPECTION_STAFF_ASSIGNMENT_NOT_ALLOWED);
            }
        }

        if (race.getStatus() == RoundStatus.SCHEDULED) {
            if (race.getStartTime() == null || race.getRound() == null
                    || race.getRound().getTournament() == null) {
                throw new AppException(ErrorCode.INSPECTION_STAFF_ASSIGNMENT_NOT_ALLOWED);
            }
            LocalDateTime inspectionOpenAt = race.getStartTime().minusMinutes(
                    race.getRound().getTournament().getInspectionOpenMinutesBefore());
            if (!LocalDateTime.now().isBefore(inspectionOpenAt)) {
                throw new AppException(ErrorCode.INSPECTION_STAFF_ASSIGNMENT_NOT_ALLOWED);
            }
        }
    }

    // Ktra phân công đua đúng dành cho 1 medic staff và 1 vet
    private boolean isSameAssignment(RaceInspectionAssignment assignment,
                                     UUID medicalStaffId, UUID veterinarianId) {
        return assignment.getMedicalStaff() != null
                && assignment.getVeterinarian() != null
                && assignment.getMedicalStaff().getMedStaffId().equals(medicalStaffId)
                && assignment.getVeterinarian().getVetId().equals(veterinarianId);
    }

    // Tự động kiếm 1 medic staff ngẫu nhiên
    private MedicalStaff lockAvailableMedicalStaff() {
        List<MedicalStaff> candidates = medicalStaffRepository.findByStatus(MedicalStaffStatus.AVAILABLE);
        if (candidates.isEmpty()) {
            throw new AppException(ErrorCode.NO_AVAILABLE_MEDICAL_STAFF);
        }
        List<UUID> candidateIds = new ArrayList<>();
        for (MedicalStaff m : candidates) {
            candidateIds.add(m.getMedStaffId());
        }
        Collections.shuffle(candidateIds);
        for (UUID candidateId : candidateIds) {
            Optional<MedicalStaff> candidate = medicalStaffRepository.findByIdForUpdate(candidateId);
            if (candidate.isPresent()
                    && candidate.get().getStatus() == MedicalStaffStatus.AVAILABLE) {
                return candidate.get();
            }
        }
        throw new AppException(ErrorCode.NO_AVAILABLE_MEDICAL_STAFF);
    }

    // Tự động kiếm 1 vet ngẫu nhiên
    private Veterinarian lockAvailableVeterinarian() {
        List<Veterinarian> candidates = veterinarianRepository.findByStatus(VetStatus.AVAILABLE);
        if (candidates.isEmpty()) {
            throw new AppException(ErrorCode.NO_AVAILABLE_VETERINARIAN);
        }
        List<UUID> candidateIds = new ArrayList<>();
        for (Veterinarian v : candidates) {
            candidateIds.add(v.getVetId());
        }
        Collections.shuffle(candidateIds);
        for (UUID candidateId : candidateIds) {
            Optional<Veterinarian> candidate = veterinarianRepository.findByIdForUpdate(candidateId);
            if (candidate.isPresent() && candidate.get().getStatus() == VetStatus.AVAILABLE) {
                return candidate.get();
            }
        }
        throw new AppException(ErrorCode.NO_AVAILABLE_VETERINARIAN);
    }
}
