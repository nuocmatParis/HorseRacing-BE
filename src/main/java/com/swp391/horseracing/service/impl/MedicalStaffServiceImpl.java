package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffCreationRequest;
import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffUpdateRequest;
import com.swp391.horseracing.dto.medical_staff.response.MedicalStaffResponse;
import com.swp391.horseracing.entity.MedicalStaff;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.MedicalStaffStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.MedicalStaffMapper;
import com.swp391.horseracing.repository.MedicalStaffRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.MedicalStaffService;
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
public class MedicalStaffServiceImpl implements MedicalStaffService {

    MedicalStaffRepository medicalStaffRepository;
    UserRepository userRepository;
    MedicalStaffMapper medicalStaffMapper;

    @Override
    @Transactional
    public MedicalStaffResponse create(MedicalStaffCreationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (medicalStaffRepository.existsByUser_UserId(user.getUserId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        MedicalStaff medicalStaff = medicalStaffMapper.toMedicalStaff(request);
        medicalStaff.setUser(user);
        medicalStaff.setStatus(MedicalStaffStatus.AVAILABLE);
        medicalStaff.setCreatedAt(LocalDateTime.now());

        return medicalStaffMapper.toMedicalStaffResponse(medicalStaffRepository.save(medicalStaff));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalStaffResponse> getAll() {
        return medicalStaffRepository.findAll()
                .stream()
                .map(medicalStaffMapper::toMedicalStaffResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalStaffResponse getById(UUID id) {
        MedicalStaff medicalStaff = medicalStaffRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_PROFILE_NOT_FOUND));
        return medicalStaffMapper.toMedicalStaffResponse(medicalStaff);
    }

    @Override
    @Transactional
    public MedicalStaffResponse update(UUID id, MedicalStaffUpdateRequest request) {
        MedicalStaff medicalStaff = medicalStaffRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_PROFILE_NOT_FOUND));

        medicalStaffMapper.updateMedicalStaff(medicalStaff, request);
        return medicalStaffMapper.toMedicalStaffResponse(medicalStaffRepository.save(medicalStaff));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        MedicalStaff medicalStaff = medicalStaffRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_STAFF_PROFILE_NOT_FOUND));
        medicalStaffRepository.delete(medicalStaff);
    }
}
