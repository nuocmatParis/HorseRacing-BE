package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.entity.RaceReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceReportMapper {

    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "raceName", source = "race.name")
    @Mapping(target = "refereeId", source = "referee.refereeId")
    @Mapping(target = "refereeName", source = "referee.user.fullName")
    @Mapping(target = "signedById", source = "signedBy.refereeId")
    @Mapping(target = "signedByName", source = "signedBy.user.fullName")
    @Mapping(target = "publishedById", source = "publishedBy.userId")
    @Mapping(target = "publishedByName", source = "publishedBy.fullName")
    RaceReportResponse toRaceReportResponse(RaceReport raceReport);
}
