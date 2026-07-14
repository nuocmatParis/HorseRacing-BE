package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateRoundRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateRoundRequest;
import com.swp391.horseracing.enums.PredictionType;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RoundMapper;
import com.swp391.horseracing.repository.RefereeRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.service.impl.RoundServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RoundPredictionTypeValidationTest {

    @Mock RoundRepository roundRepository;
    @Mock TournamentRepository tournamentRepository;
    @Mock UserRepository userRepository;
    @Mock RefereeRepository refereeRepository;
    @Mock RoundMapper roundMapper;

    @InjectMocks RoundServiceImpl roundService;

    @Test
    void createRejectsTopOnePredictionType() {
        CreateRoundRequest request = CreateRoundRequest.builder()
                .predictionType(PredictionType.TOP1)
                .build();

        AppException exception = assertThrows(
                AppException.class,
                () -> roundService.create(UUID.randomUUID(), request));

        assertEquals(ErrorCode.INVALID_PREDICTION_TYPE, exception.getErrorCode());
    }

    @Test
    void updateRejectsTopOnePredictionType() {
        UpdateRoundRequest request = UpdateRoundRequest.builder()
                .predictionType(PredictionType.TOP1)
                .build();

        AppException exception = assertThrows(
                AppException.class,
                () -> roundService.update(UUID.randomUUID(), request));

        assertEquals(ErrorCode.INVALID_PREDICTION_TYPE, exception.getErrorCode());
    }
}
