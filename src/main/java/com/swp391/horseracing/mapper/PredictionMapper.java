package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.prediction.response.PredictionDetailResponse;
import com.swp391.horseracing.dto.prediction.response.PredictionResponse;
import com.swp391.horseracing.entity.Prediction;
import com.swp391.horseracing.entity.PredictionDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PredictionMapper {

    @Mapping(target = "spectatorId", source = "spectator.spectatorId")
    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "details", source = "predictionDetails")
    @Mapping(target = "aiPredictions", ignore = true)
    PredictionResponse toPredictionResponse(Prediction prediction);

    @Mapping(target = "entryId", source = "entry.entryId")
    PredictionDetailResponse toPredictionDetailResponse(PredictionDetail detail);

    List<PredictionDetailResponse> toPredictionDetailResponseList(List<PredictionDetail> details);

    @Mapping(target = "aiPredictions", ignore = true)
    List<PredictionResponse> toPredictionResponseList(List<Prediction> predictions);
}
