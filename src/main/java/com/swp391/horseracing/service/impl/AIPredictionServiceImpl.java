package com.swp391.horseracing.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.horseracing.dto.prediction.response.AIPredictionResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.TrackCondition;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AIPredictionMapper;
import com.swp391.horseracing.repository.AIPredictionRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.service.AIClientService;
import com.swp391.horseracing.service.AIPredictionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class AIPredictionServiceImpl implements AIPredictionService {

    AIPredictionRepository aiPredictionRepository;
    RaceRepository raceRepository;
    RaceEntryRepository raceEntryRepository;
    AIClientService aiClientService;
    AIPredictionMapper aiPredictionMapper;
    ObjectMapper objectMapper = new ObjectMapper();

    private static final String MODEL_VERSION = "gpt-4o-mini-v1";

    @Override
    @Transactional
    public List<AIPredictionResponse> generatePredictions(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId);
        if (entries.isEmpty()) {
            throw new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND);
        }

        List<Map<String, Object>> entryDataList = buildEntryData(entries);
        String prompt = buildPrompt(race, entryDataList);

        String aiResponse;
        try {
            aiResponse = aiClientService.predict(prompt);
        } catch (Exception e) {
            log.error("AI prediction failed for race {}: {}", raceId, e.getMessage());
            throw new AppException(ErrorCode.AI_PREDICTION_GENERATION_FAILED);
        }

        List<Map<String, Object>> aiResults = parseAiResponse(aiResponse, entries.size());

        // Remove old predictions for this race
        List<AIPrediction> oldPredictions = aiPredictionRepository.findByEntry_Race_RaceId(raceId);
        if (!oldPredictions.isEmpty()) {
            aiPredictionRepository.deleteAll(oldPredictions);
        }

        int numberOfCompetitors = entries.size();
        TrackCondition trackCondition = mapTrackCondition(race.getTrackCondition());
        int raceDistance = race.getDistance() != null ? race.getDistance().intValue() : 0;

        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        for (Map<String, Object> result : aiResults) {
            resultMap.put(result.get("entryId").toString(), result);
        }

        int maxRating = entries.stream()
                .mapToInt(e -> e.getContract().getHorse().getCurrentRating())
                .max().orElse(1);

        List<AIPrediction> predictions = new ArrayList<>();
        for (RaceEntry entry : entries) {
            String entryIdStr = entry.getEntryId().toString();
            Map<String, Object> result = resultMap.get(entryIdStr);

            int predictedRank = 0;
            BigDecimal winProbability = BigDecimal.ZERO;
            BigDecimal confidenceScore = BigDecimal.ZERO;

            if (result != null) {
                predictedRank = ((Number) result.getOrDefault("predictedRank", 0)).intValue();
                winProbability = BigDecimal.valueOf(
                        ((Number) result.getOrDefault("winProbability", 0)).doubleValue())
                        .setScale(2, RoundingMode.HALF_UP);
                confidenceScore = BigDecimal.valueOf(
                        ((Number) result.getOrDefault("confidenceScore", 0)).doubleValue())
                        .setScale(2, RoundingMode.HALF_UP);
            }

            Horse horse = entry.getContract().getHorse();
            Jockey jockey = entry.getContract().getJockey();

            BigDecimal horseWinRate = BigDecimal.valueOf(horse.getWinRate() != null ? horse.getWinRate() : 0.0)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal horseCurrentRating = BigDecimal.valueOf(horse.getCurrentRating())
                    .setScale(2, RoundingMode.HALF_UP);

            int totalRaces = horse.getTotalRaces();
            BigDecimal horseTop3Rate = totalRaces > 0
                    ? BigDecimal.valueOf((double) horse.getTotalTop3Finishes() / totalRaces * 100)
                    .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal horseRecentForm = totalRaces > 0
                    ? BigDecimal.valueOf((double) horse.getTotalWins() / totalRaces * 100)
                    .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            int jockeyTotalRaces = jockey.getTotalRaces();
            BigDecimal jockeyWinRate = jockeyTotalRaces > 0
                    ? BigDecimal.valueOf((double) jockey.getTotalWins() / jockeyTotalRaces * 100)
                    .setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal jockeyTop3Rate = BigDecimal.ZERO;
            BigDecimal jockeyRecentForm = jockeyWinRate;

            BigDecimal pairWinRate = horseWinRate;
            BigDecimal pairTop3Rate = horseTop3Rate;

            BigDecimal relativeRating = BigDecimal.valueOf((double) horse.getCurrentRating() / maxRating * 100)
                    .setScale(2, RoundingMode.HALF_UP);

            AIPrediction prediction = AIPrediction.builder()
                    .entry(entry)
                    .horseCurrentRating(horseCurrentRating)
                    .horseRecentForm(horseRecentForm)
                    .horseWinRate(horseWinRate)
                    .horseTop3Rate(horseTop3Rate)
                    .jockeyWinRate(jockeyWinRate)
                    .jockeyTop3Rate(jockeyTop3Rate)
                    .jockeyRecentForm(jockeyRecentForm)
                    .pairWinRate(pairWinRate)
                    .pairTop3Rate(pairTop3Rate)
                    .raceDistance(raceDistance)
                    .trackCondition(trackCondition)
                    .numberOfCompetitors(numberOfCompetitors)
                    .laneNumber(entry.getLaneNumber())
                    .assignedWeightKg(BigDecimal.ZERO)
                    .actualCarriedWeightKg(BigDecimal.ZERO)
                    .carriedWeightRatio(BigDecimal.ZERO)
                    .relativeRating(relativeRating)
                    .winProbability(winProbability)
                    .predictedRank(predictedRank)
                    .confidenceScore(confidenceScore)
                    .modelVersion(MODEL_VERSION)
                    .generatedAt(LocalDateTime.now())
                    .build();

            predictions.add(prediction);
        }

        List<AIPrediction> saved = aiPredictionRepository.saveAll(predictions);
        return aiPredictionMapper.toAIPredictionResponseList(saved.stream()
                .sorted(Comparator.comparingInt(AIPrediction::getPredictedRank))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIPredictionResponse> getPredictionsByRace(UUID raceId) {
        List<AIPrediction> predictions = aiPredictionRepository.findByEntry_Race_RaceId(raceId);
        predictions.sort(Comparator.comparingInt(AIPrediction::getPredictedRank));
        return aiPredictionMapper.toAIPredictionResponseList(predictions);
    }

    private List<Map<String, Object>> buildEntryData(List<RaceEntry> entries) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (RaceEntry entry : entries) {
            Horse horse = entry.getContract().getHorse();
            Jockey jockey = entry.getContract().getJockey();

            int jTotalRaces = jockey.getTotalRaces();
            double jockeyWinRate = jTotalRaces > 0
                    ? (double) jockey.getTotalWins() / jTotalRaces * 100 : 0;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("entryId", entry.getEntryId().toString());
            data.put("lane", entry.getLaneNumber());
            data.put("horseName", horse.getName());
            data.put("horseRating", horse.getCurrentRating());
            data.put("horseWinRate", horse.getWinRate());
            data.put("horseTotalRaces", horse.getTotalRaces());
            data.put("horseTotalWins", horse.getTotalWins());
            data.put("horseTotalTop3", horse.getTotalTop3Finishes());
            data.put("jockeyName", jockey.getUser().getFullName());
            data.put("jockeyWinRate", Math.round(jockeyWinRate * 100.0) / 100.0);
            data.put("jockeyTotalRaces", jTotalRaces);
            data.put("jockeyTotalWins", jockey.getTotalWins());
            data.put("jockeyExperience", jockey.getExperienceYears());
            dataList.add(data);
        }
        return dataList;
    }

    private String buildPrompt(Race race, List<Map<String, Object>> entryDataList) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a professional horse racing analyst. Predict the outcome of the following race.\n\n");
        sb.append("Race: ").append(race.getName()).append("\n");
        sb.append("Distance: ").append(race.getDistance() != null ? race.getDistance().intValue() : 0).append("m\n");
        sb.append("Track Condition: ").append(race.getTrackCondition()).append("\n");
        sb.append("Number of competitors: ").append(entryDataList.size()).append("\n\n");

        sb.append("Entries (sorted by lane):\n");
        sb.append("Lane | Horse              | Rating | WinRate% | TotalRc | Top3  | Jockey             | JocWin% | ExpYr\n");
        sb.append("-----+--------------------+--------+----------+---------+-------+--------------------+---------+------\n");

        for (Map<String, Object> d : entryDataList) {
            String hn = ((String) d.get("horseName"));
            hn = hn.length() > 18 ? hn.substring(0, 18) : String.format("%-18s", hn);
            String jn = ((String) d.get("jockeyName"));
            jn = jn.length() > 18 ? jn.substring(0, 18) : String.format("%-18s", jn);

            sb.append(String.format("%-5s %-18s %-7s %-9s %-8s %-6s %-18s %-8s %s%n",
                    d.get("lane"), hn,
                    d.get("horseRating"), d.get("horseWinRate"),
                    d.get("horseTotalRaces"), d.get("horseTotalTop3"),
                    jn, d.get("jockeyWinRate"), d.get("jockeyExperience")));
        }

        sb.append("\nBased on this data, predict the final ranking for ALL entries. ");
        sb.append("For each entry, provide:\n");
        sb.append("- predictedRank (1, 2, 3, ...)\n");
        sb.append("- winProbability (0.00 to 100.00, likelihood of winning)\n");
        sb.append("- confidenceScore (0.00 to 100.00, confidence in this prediction)\n\n");
        sb.append("Return ONLY a valid JSON array. No explanation, no markdown:\n");
        sb.append("[\n");
        sb.append("  {\"entryId\": \"...\", \"predictedRank\": 1, \"winProbability\": 35.50, \"confidenceScore\": 80.00},\n");
        sb.append("  ...\n");
        sb.append("]");

        return sb.toString();
    }

    private List<Map<String, Object>> parseAiResponse(String aiResponse, int expectedCount) {
        String json = aiResponse.trim();

        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) {
                json = json.substring(start, end).trim();
            }
        }

        int arrayStart = json.indexOf('[');
        int arrayEnd = json.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            json = json.substring(arrayStart, arrayEnd + 1);
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                log.error("AI response is not a JSON array: {}", json);
                throw new AppException(ErrorCode.AI_PREDICTION_INVALID_RESPONSE);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode node : root) {
                Map<String, Object> map = new HashMap<>();
                map.put("entryId", node.get("entryId").asText());
                map.put("predictedRank", node.get("predictedRank").asInt());
                map.put("winProbability", node.get("winProbability").asDouble());
                map.put("confidenceScore", node.get("confidenceScore").asDouble());
                results.add(map);
            }

            if (results.isEmpty()) {
                log.error("AI returned empty predictions array");
                throw new AppException(ErrorCode.AI_PREDICTION_INVALID_RESPONSE);
            }

            return results;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response: {}", aiResponse, e);
            throw new AppException(ErrorCode.AI_PREDICTION_INVALID_RESPONSE);
        }
    }

    private TrackCondition mapTrackCondition(String trackCondition) {
        if (trackCondition == null) return TrackCondition.TURF;
        try {
            return TrackCondition.valueOf(trackCondition.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown track condition '{}', defaulting to TURF", trackCondition);
            return TrackCondition.TURF;
        }
    }
}
