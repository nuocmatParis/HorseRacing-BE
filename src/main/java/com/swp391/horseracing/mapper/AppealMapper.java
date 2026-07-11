package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.appeal.response.AppealResponse;
import com.swp391.horseracing.entity.Appeal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppealMapper {

    @Mapping(target = "entryId", source = "entry.entryId")
    @Mapping(target = "raceResultId", source = "raceResult.resultId")
    @Mapping(target = "relatedViolationId", source = "relatedViolation.violationId")
    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "submittedByUserId", source = "submittedBy.userId")
    @Mapping(target = "reviewedByRefereeId", source = "reviewedBy.refereeId")
    AppealResponse toAppealResponse(Appeal appeal);
}
