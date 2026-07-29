package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_report.request.ReturnRaceReportRequest;
import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.PrizeStructureMapper;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.impl.PrizeStructureServiceImpl;
import com.swp391.horseracing.service.impl.RaceReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefereeBusinessRulesTest {

    @Mock
    private PrizeStructureRepository prizeStructureRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private PrizeStructureMapper prizeStructureMapper;

    @InjectMocks
    private PrizeStructureServiceImpl prizeStructureService;

    @InjectMocks
    private RaceReportServiceImpl raceReportService;

    private UUID tournamentId;
    private Tournament tournament;

    @BeforeEach
    void setUp() {
        tournamentId = UUID.randomUUID();
        tournament = Tournament.builder()
                .tournamentId(tournamentId)
                .status(TournamentStatus.DRAFT)
                .maxApprovedHorses(5)
                .maxEntriesPerRace(16)
                .build();
    }

    @Test
    @DisplayName("Chặn khi tạo Prize Structure với Rank > Số lượng ngựa cho phép")
    void testCreatePrizeStructure_RankExceedsMaxHorses_ThrowsException() {
        CreatePrizeStructureRequest request = new CreatePrizeStructureRequest();
        request.setRank(6); // Max approved horses is 5
        request.setPercentage(20.0f);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        AppException exception = assertThrows(AppException.class, () ->
                prizeStructureService.create(tournamentId, request)
        );

        assertEquals(ErrorCode.PRIZE_RANK_EXCEEDS_HORSE_COUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("Cho phép tạo Prize Structure khi Rank <= Số lượng ngựa cho phép")
    void testCreatePrizeStructure_RankWithinMaxHorses_Passes() {
        CreatePrizeStructureRequest request = new CreatePrizeStructureRequest();
        request.setRank(3); // Within max approved horses (5)
        request.setPercentage(20.0f);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(prizeStructureRepository.existsByTournament_TournamentIdAndRank(tournamentId, 3)).thenReturn(false);
        when(prizeStructureRepository.findByTournament_TournamentId(tournamentId)).thenReturn(Collections.emptyList());
        when(prizeStructureMapper.toPrizeStructure(any())).thenReturn(new PrizeStructure());

        assertDoesNotThrow(() -> prizeStructureService.create(tournamentId, request));
    }

    @Test
    @DisplayName("Kiểm tra Trọng tài chính gọi returnReport sẽ bị chặn với mã lỗi RACE_REPORT_RETURN_NOT_ALLOWED")
    void testHeadReferee_ReturnReport_Disabled() {
        ReturnRaceReportRequest request = new ReturnRaceReportRequest();
        request.setReason("Cần kiểm tra lại");

        AppException exception = assertThrows(AppException.class, () ->
                raceReportService.returnReport(UUID.randomUUID(), request)
        );

        assertEquals(ErrorCode.RACE_REPORT_RETURN_NOT_ALLOWED, exception.getErrorCode());
    }
}
