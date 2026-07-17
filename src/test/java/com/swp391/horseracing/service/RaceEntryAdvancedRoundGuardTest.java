package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceEntryMapper;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.service.impl.RaceEntryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceEntryAdvancedRoundGuardTest {

    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceRepository raceRepository;
    @Mock RoundRepository roundRepository;
    @Mock JockeyHorseContractRepository contractRepository;
    @Mock RaceEntryMapper raceEntryMapper;
    @Mock UserCurrentService userCurrentService;

    @InjectMocks RaceEntryServiceImpl service;

    private Round advancedRound;
    private Race advancedRace;

    @BeforeEach
    void setUp() {
        Tournament tournament = new Tournament();
        tournament.setTournamentId(UUID.randomUUID());
        tournament.setPhase(TournamentPhase.SCHEDULING);

        advancedRound = new Round();
        advancedRound.setRoundId(UUID.randomUUID());
        advancedRound.setSequenceOrder(2);
        advancedRound.setTournament(tournament);

        advancedRace = new Race();
        advancedRace.setRaceId(UUID.randomUUID());
        advancedRace.setRound(advancedRound);
        advancedRace.setStatus(RoundStatus.SCHEDULING);
    }

    @Test
    void manualCreateCannotAddLoserToAdvancedRound() {
        CreateRaceEntryRequest request = CreateRaceEntryRequest.builder()
                .raceId(advancedRace.getRaceId())
                .contractId(UUID.randomUUID())
                .build();
        when(raceRepository.findById(advancedRace.getRaceId()))
                .thenReturn(Optional.of(advancedRace));

        AppException exception = assertThrows(AppException.class, () -> service.create(request));

        assertEquals(ErrorCode.ADVANCED_ROUND_ENTRIES_MANAGED_BY_RESULTS,
                exception.getErrorCode());
        verify(contractRepository, never()).findById(request.getContractId());
    }

    @Test
    void autoAssignCannotPullAllApprovedContractsIntoAdvancedRound() {
        when(roundRepository.findById(advancedRound.getRoundId()))
                .thenReturn(Optional.of(advancedRound));

        AppException exception = assertThrows(AppException.class,
                () -> service.autoAssignRound(advancedRound.getRoundId()));

        assertEquals(ErrorCode.ADVANCED_ROUND_ENTRIES_MANAGED_BY_RESULTS,
                exception.getErrorCode());
        verify(raceRepository, never()).findByRound_RoundId(advancedRound.getRoundId());
    }

    @Test
    void qualifierEntryCannotBeDeletedFromAdvancedRound() {
        RaceEntry entry = new RaceEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setRace(advancedRace);
        when(raceEntryRepository.findById(entry.getEntryId())).thenReturn(Optional.of(entry));

        AppException exception = assertThrows(AppException.class,
                () -> service.delete(entry.getEntryId()));

        assertEquals(ErrorCode.ADVANCED_ROUND_ENTRIES_MANAGED_BY_RESULTS,
                exception.getErrorCode());
        verify(raceEntryRepository, never()).delete(entry);
    }
}
