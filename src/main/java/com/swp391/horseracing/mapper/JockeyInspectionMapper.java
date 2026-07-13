package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.jockeyinspection.response.JockeyInspectionResponse;
import com.swp391.horseracing.entity.JockeyInspection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JockeyInspectionMapper {

    @Mapping(target = "entryId", source = "raceEntry.entryId")
    @Mapping(target = "jockeyId", source = "raceEntry.contract.jockey.jockeyId")
    @Mapping(target = "jockeyName", source = "raceEntry.contract.jockey.user.fullName")
    @Mapping(target = "medStaffId", source = "medicalStaff.medStaffId")
    @Mapping(target = "medicalStaffName", source = "medicalStaff.user.fullName")
    @Mapping(target = "certification", source = "medicalStaff.certification")
    JockeyInspectionResponse toResponse(JockeyInspection inspection);
}
