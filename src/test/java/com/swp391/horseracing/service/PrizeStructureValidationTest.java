package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.impl.PrizeStructureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrizeStructureValidationTest {

    @Mock
    private PrizeStructureRepository prizeStructureRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private PrizeStructureServiceImpl prizeStructureService;

    private UUID tournamentId;

    @BeforeEach
    void setUp() {
        tournamentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Validate Prize Structure - Valid (Top 1: 50%, Top 2: 30%, Top 3: 20%) -> Pass")
    void testValidatePrizeStructure_Valid_Passes() {
        Tournament tournament = Tournament.builder().tournamentId(tournamentId).build();

        PrizeStructure rank1 = PrizeStructure.builder()
                .prizeStructureId(UUID.randomUUID())
                .rank(1)
                .percentage(50.0f)
                .fixedAmount(BigDecimal.valueOf(5000))
                .isActive(true)
                .tournament(tournament)
                .build();

        PrizeStructure rank2 = PrizeStructure.builder()
                .prizeStructureId(UUID.randomUUID())
                .rank(2)
                .percentage(30.0f)
                .fixedAmount(BigDecimal.valueOf(3000))
                .isActive(true)
                .tournament(tournament)
                .build();

        PrizeStructure rank3 = PrizeStructure.builder()
                .prizeStructureId(UUID.randomUUID())
                .rank(3)
                .percentage(20.0f)
                .fixedAmount(BigDecimal.valueOf(2000))
                .isActive(true)
                .tournament(tournament)
                .build();

        when(prizeStructureRepository.findByTournament_TournamentId(tournamentId))
                .thenReturn(Arrays.asList(rank1, rank2, rank3));

        assertDoesNotThrow(() -> prizeStructureService.validatePrizeStructuresForTournament(tournamentId));
    }

    @Test
    @DisplayName("Validate Prize Structure - Total < 100% (80%) -> Throws INVALID_TOTAL_PRIZE_PERCENTAGE")
    void testValidatePrizeStructure_TotalLessThan100_ThrowsException() {
        PrizeStructure rank1 = PrizeStructure.builder()
                .rank(1)
                .percentage(50.0f)
                .isActive(true)
                .build();

        PrizeStructure rank2 = PrizeStructure.builder()
                .rank(2)
                .percentage(30.0f)
                .isActive(true)
                .build();

        when(prizeStructureRepository.findByTournament_TournamentId(tournamentId))
                .thenReturn(Arrays.asList(rank1, rank2));

        AppException exception = assertThrows(AppException.class,
                () -> prizeStructureService.validatePrizeStructuresForTournament(tournamentId));

        assertEquals(ErrorCode.INVALID_TOTAL_PRIZE_PERCENTAGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Validate Prize Structure - Total > 100% (110%) -> Throws INVALID_TOTAL_PRIZE_PERCENTAGE")
    void testValidatePrizeStructure_TotalGreaterThan100_ThrowsException() {
        PrizeStructure rank1 = PrizeStructure.builder()
                .rank(1)
                .percentage(50.0f)
                .isActive(true)
                .build();

        PrizeStructure rank2 = PrizeStructure.builder()
                .rank(2)
                .percentage(40.0f)
                .isActive(true)
                .build();

        PrizeStructure rank3 = PrizeStructure.builder()
                .rank(3)
                .percentage(20.0f)
                .isActive(true)
                .build();

        when(prizeStructureRepository.findByTournament_TournamentId(tournamentId))
                .thenReturn(Arrays.asList(rank1, rank2, rank3));

        AppException exception = assertThrows(AppException.class,
                () -> prizeStructureService.validatePrizeStructuresForTournament(tournamentId));

        assertEquals(ErrorCode.INVALID_TOTAL_PRIZE_PERCENTAGE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Create Prize Structure - Equal percentages (Rank 1: 40%, creating Rank 2: 40%) -> Throws INVALID_PRIZE_RANK_HIERARCHY")
    void testCreate_EqualPercentages_ThrowsException() {
        Tournament tournament = Tournament.builder()
                .tournamentId(tournamentId)
                .status(com.swp391.horseracing.enums.TournamentStatus.DRAFT)
                .build();

        PrizeStructure rank1 = PrizeStructure.builder()
                .rank(1)
                .percentage(40.0f)
                .isActive(true)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(prizeStructureRepository.findByTournament_TournamentId(tournamentId))
                .thenReturn(Collections.singletonList(rank1));

        CreatePrizeStructureRequest request = CreatePrizeStructureRequest.builder()
                .rank(2)
                .percentage(40.0f)
                .fixedAmount(BigDecimal.valueOf(20000000))
                .isActive(true)
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> prizeStructureService.create(tournamentId, request));

        assertEquals(ErrorCode.INVALID_PRIZE_RANK_HIERARCHY, exception.getErrorCode());
    }

    @Test
    @DisplayName("Create Prize Structure - Rank 2 higher than Rank 1 (Rank 1: 30%, Rank 2: 39.99%) -> Throws INVALID_PRIZE_RANK_HIERARCHY immediately")
    void testCreate_Rank2HigherThanRank1_ThrowsException() {
        Tournament tournament = Tournament.builder()
                .tournamentId(tournamentId)
                .status(com.swp391.horseracing.enums.TournamentStatus.DRAFT)
                .build();

        PrizeStructure rank1 = PrizeStructure.builder()
                .rank(1)
                .percentage(30.0f)
                .isActive(true)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(prizeStructureRepository.findByTournament_TournamentId(tournamentId))
                .thenReturn(Collections.singletonList(rank1));

        CreatePrizeStructureRequest request = CreatePrizeStructureRequest.builder()
                .rank(2)
                .percentage(39.99f)
                .fixedAmount(BigDecimal.valueOf(19995000))
                .isActive(true)
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> prizeStructureService.create(tournamentId, request));

        assertEquals(ErrorCode.INVALID_PRIZE_RANK_HIERARCHY, exception.getErrorCode());
    }
}
