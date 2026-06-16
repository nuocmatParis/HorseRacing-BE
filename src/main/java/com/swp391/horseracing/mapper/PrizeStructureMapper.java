package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.dto.tournament.response.PrizeStructureResponse;
import com.swp391.horseracing.entity.PrizeStructure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PrizeStructureMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    PrizeStructureResponse toPrizeStructureResponse(PrizeStructure prizeStructure);

    @Mapping(target = "prizeStructureId", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    PrizeStructure toPrizeStructure(CreatePrizeStructureRequest request);
}
