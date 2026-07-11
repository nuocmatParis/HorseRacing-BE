package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.prediction.response.AIPredictionResponse;
import com.swp391.horseracing.entity.AIPrediction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AIPredictionMapper {

    @Mapping(target = "entryId", source = "entry.entryId")
    @Mapping(target = "laneNumber", source = "entry.laneNumber")
    @Mapping(target = "horseName", source = "entry.contract.horse.name")
    @Mapping(target = "jockeyName", source = "entry.contract.jockey.user.fullName")
    @Mapping(target = "horseRating", source = "entry.contract.horse.currentRating")
    AIPredictionResponse toAIPredictionResponse(AIPrediction aiPrediction);

    List<AIPredictionResponse> toAIPredictionResponseList(List<AIPrediction> aiPredictions);
}
