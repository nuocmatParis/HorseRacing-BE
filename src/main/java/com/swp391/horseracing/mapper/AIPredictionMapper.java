package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.prediction.response.AIPredictionResponse;
import com.swp391.horseracing.entity.AIPrediction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AIPredictionMapper {

    @Mapping(target = "entryId", source = "entry.entryId")
    AIPredictionResponse toAIPredictionResponse(AIPrediction aiPrediction);

    List<AIPredictionResponse> toAIPredictionResponseList(List<AIPrediction> aiPredictions);
}
