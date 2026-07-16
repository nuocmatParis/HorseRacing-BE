package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.enums.AIPredictionPublicationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.AIPredictionMapper;
import com.swp391.horseracing.repository.AIPredictionRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.service.impl.AIPredictionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIPredictionPublicationWorkflowTest {

    @Mock AIPredictionRepository aiPredictionRepository;
    @Mock RaceRepository raceRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock AIClientService aiClientService;
    @Mock AIPredictionMapper aiPredictionMapper;
    @Mock UserCurrentService userCurrentService;
    @InjectMocks AIPredictionServiceImpl service;

    @Test
    void spectatorCannotReadDraftPrediction() {
        Race race = eligibleRace();
        race.setAiPredictionPublicationStatus(AIPredictionPublicationStatus.DRAFT);
        when(raceRepository.findById(race.getRaceId())).thenReturn(Optional.of(race));

        AppException exception = assertThrows(AppException.class,
                () -> service.getPublishedPredictionsByRace(race.getRaceId()));

        assertEquals(ErrorCode.AI_PREDICTION_NOT_PUBLISHED, exception.getErrorCode());
    }

    @Test
    void spectatorCanReadPublishedPrediction() {
        Race race = eligibleRace();
        race.setAiPredictionPublicationStatus(AIPredictionPublicationStatus.PUBLISHED);
        when(raceRepository.findById(race.getRaceId())).thenReturn(Optional.of(race));
        when(aiPredictionRepository.findByEntry_Race_RaceId(race.getRaceId()))
                .thenReturn(new ArrayList<>());
        when(aiPredictionMapper.toAIPredictionResponseList(new ArrayList<>()))
                .thenReturn(new ArrayList<>());

        var response = service.getPublishedPredictionsByRace(race.getRaceId());

        assertEquals(AIPredictionPublicationStatus.PUBLISHED, response.getPublicationStatus());
        assertTrue(response.getPredictions().isEmpty());
    }

    @Test
    void adminCannotGenerateForStartedRace() {
        Race race = eligibleRace();
        race.setStatus(RoundStatus.ONGOING);
        when(raceRepository.findForUpdateByRaceId(race.getRaceId())).thenReturn(Optional.of(race));

        AppException exception = assertThrows(AppException.class,
                () -> service.generatePredictions(race.getRaceId(), 3));

        assertEquals(ErrorCode.AI_PREDICTION_RACE_NOT_ELIGIBLE, exception.getErrorCode());
    }

    @Test
    void adminCannotPublishForCancelledRace() {
        Race race = eligibleRace();
        race.setStatus(RoundStatus.CANCELLED);
        when(raceRepository.findForUpdateByRaceId(race.getRaceId())).thenReturn(Optional.of(race));

        AppException exception = assertThrows(AppException.class,
                () -> service.publishPredictions(race.getRaceId()));

        assertEquals(ErrorCode.AI_PREDICTION_RACE_NOT_ELIGIBLE, exception.getErrorCode());
    }

    private Race eligibleRace() {
        Race race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setName("Race");
        race.setStatus(RoundStatus.SCHEDULED);
        race.setSchedulePublishedAt(LocalDateTime.now().minusHours(1));
        race.setStartTime(LocalDateTime.now().plusHours(1));
        return race;
    }
}
