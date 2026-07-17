package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.appeal.response.AppealResponse;
import com.swp391.horseracing.entity.Appeal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppealMapper {

    @Mapping(target = "entryId", source = "entry.entryId")
    @Mapping(target = "raceId", source = "entry.race.raceId")
    @Mapping(target = "raceName", source = "entry.race.name")
    @Mapping(target = "roundName", source = "entry.race.round.roundName")
    @Mapping(target = "tournamentName", source = "entry.race.round.tournament.name")
    @Mapping(target = "horseName", source = "entry.contract.horse.name")
    @Mapping(target = "jockeyName", source = "entry.contract.jockey.user.fullName")
    @Mapping(target = "raceResultId", source = "raceResult.resultId")
    @Mapping(target = "relatedViolationId", source = "relatedViolation.violationId")
    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "submittedByUserId", source = "submittedBy.userId")
    @Mapping(target = "reviewedByRefereeId", source = "reviewedBy.refereeId")
    AppealResponse toAppealResponse(Appeal appeal);
}
