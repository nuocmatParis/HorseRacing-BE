package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.violation.response.ViolationResponse;
import com.swp391.horseracing.entity.Violation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ViolationMapper {

    @Mapping(target = "entryId", source = "entry.entryId")
    @Mapping(target = "refereeId", source = "referee.refereeId")
    ViolationResponse toViolationResponse(Violation violation);
}
