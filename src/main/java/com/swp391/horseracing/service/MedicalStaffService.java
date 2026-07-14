package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffCreationRequest;
import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffUpdateRequest;
import com.swp391.horseracing.dto.medical_staff.response.MedicalStaffResponse;

import java.util.List;
import java.util.UUID;

public interface MedicalStaffService {

    MedicalStaffResponse create(MedicalStaffCreationRequest request);

    List<MedicalStaffResponse> getAll();

    MedicalStaffResponse getById(UUID id);

    MedicalStaffResponse update(UUID id, MedicalStaffUpdateRequest request);

    void delete(UUID id);
}
