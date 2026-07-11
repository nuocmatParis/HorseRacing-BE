package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.entity.RaceReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceReportMapper {

    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "refereeId", source = "referee.refereeId")
    @Mapping(target = "signedById", source = "signedBy.refereeId")
    @Mapping(target = "publishedById", source = "publishedBy.userId")
    RaceReportResponse toRaceReportResponse(RaceReport raceReport);
}
