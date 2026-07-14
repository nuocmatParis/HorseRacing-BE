package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffCreationRequest;
import com.swp391.horseracing.dto.medical_staff.request.MedicalStaffUpdateRequest;
import com.swp391.horseracing.dto.medical_staff.response.MedicalStaffResponse;
import com.swp391.horseracing.entity.MedicalStaff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MedicalStaffMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "username", source = "user.username")
    MedicalStaffResponse toMedicalStaffResponse(MedicalStaff medicalStaff);

    @Mapping(target = "medStaffId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MedicalStaff toMedicalStaff(MedicalStaffCreationRequest request);

    @Mapping(target = "medStaffId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateMedicalStaff(@MappingTarget MedicalStaff medicalStaff, MedicalStaffUpdateRequest request);
}
