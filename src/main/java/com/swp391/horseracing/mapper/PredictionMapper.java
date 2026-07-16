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
    @Mapping(target = "horseId", source = "entry.contract.horse.horseId")
    @Mapping(target = "horseName", source = "entry.contract.horse.name")
    @Mapping(target = "jockeyId", source = "entry.contract.jockey.jockeyId")
    @Mapping(target = "jockeyName", source = "entry.contract.jockey.user.fullName")
    @Mapping(target = "laneNumber", source = "entry.laneNumber")
    PredictionDetailResponse toPredictionDetailResponse(PredictionDetail detail);

    List<PredictionDetailResponse> toPredictionDetailResponseList(List<PredictionDetail> details);

    @Mapping(target = "aiPredictions", ignore = true)
    List<PredictionResponse> toPredictionResponseList(List<Prediction> predictions);
}
