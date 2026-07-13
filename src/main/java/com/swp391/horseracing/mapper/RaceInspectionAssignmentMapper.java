package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.medicalstaff.response.InspectionStaffAssignmentResponse;
import com.swp391.horseracing.entity.RaceInspectionAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceInspectionAssignmentMapper {

    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "raceName", source = "race.name")
    @Mapping(target = "veterinarianId", source = "veterinarian.vetId")
    @Mapping(target = "veterinarianName", source = "veterinarian.user.fullName")
    @Mapping(target = "medStaffId", source = "medicalStaff.medStaffId")
    @Mapping(target = "medicalStaffName", source = "medicalStaff.user.fullName")
    @Mapping(target = "certification", source = "medicalStaff.certification")
    @Mapping(target = "yearsOfService", source = "medicalStaff.yearsOfService")
    @Mapping(target = "assignedById", source = "assignedBy.userId")
    @Mapping(target = "assignedByName", source = "assignedBy.fullName")
    InspectionStaffAssignmentResponse toInspectionStaffAssignmentResponse(RaceInspectionAssignment assignment);
}
