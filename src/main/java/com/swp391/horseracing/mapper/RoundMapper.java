package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.request.CreateRoundRequest;
import com.swp391.horseracing.dto.tournament.response.RoundResponse;
import com.swp391.horseracing.entity.Round;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoundMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    @Mapping(target = "createdById", source = "createdBy.userId")
    RoundResponse toRoundResponse(Round round);

    @Mapping(target = "roundId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "races", ignore = true)
    Round toRound(CreateRoundRequest request);
}
