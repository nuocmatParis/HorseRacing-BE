package com.swp391.horseracing.service;

import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.mapper.RaceEntryMapper;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.service.impl.RaceEntryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceEntryLaneAssignmentTest {

    @Mock RaceEntryRepository raceEntryRepository;
    @Mock RaceRepository raceRepository;
    @Mock RoundRepository roundRepository;
    @Mock JockeyHorseContractRepository contractRepository;
    @Mock RaceEntryMapper raceEntryMapper;
    @Mock UserCurrentService userCurrentService;

    @InjectMocks RaceEntryServiceImpl raceEntryService;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void autoAssignRoundCreatesEntriesWithoutLanes() {
        Tournament tournament = new Tournament();
        tournament.setTournamentId(UUID.randomUUID());
        tournament.setPhase(TournamentPhase.SCHEDULING);

        Round round = new Round();
        round.setRoundId(UUID.randomUUID());
        round.setTournament(tournament);

        Race firstRace = schedulingRace(round);
        Race secondRace = schedulingRace(round);
        List<JockeyHorseContract> contracts = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            JockeyHorseContract contract = new JockeyHorseContract();
            contract.setContractId(UUID.randomUUID());
            contracts.add(contract);
        }

        when(roundRepository.findById(round.getRoundId())).thenReturn(Optional.of(round));
        when(raceRepository.findByRound_RoundId(round.getRoundId()))
                .thenReturn(List.of(firstRace, secondRace));
        when(contractRepository.findByTournament_TournamentIdAndStatus(
                tournament.getTournamentId(), ContractStatus.APPROVED)).thenReturn(contracts);
        when(raceEntryRepository.findByRace_Round_RoundId(round.getRoundId()))
                .thenReturn(List.of());
        when(userCurrentService.getCurrentUser()).thenReturn(new User());

        raceEntryService.autoAssignRound(round.getRoundId());

        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(raceEntryRepository).saveAll(captor.capture());
        int entryCount = 0;
        for (Object value : captor.getValue()) {
            RaceEntry entry = (RaceEntry) value;
            assertNull(entry.getLaneNumber());
            entryCount++;
        }
        assertEquals(4, entryCount);
    }

    @Test
    void autoAssignLanesClearsOldLanesBeforeReassigning() {
        Round round = new Round();
        Race race = schedulingRace(round);
        RaceEntry firstEntry = raceEntry(race, 1);
        RaceEntry secondEntry = raceEntry(race, 2);
        List<RaceEntry> entries = new ArrayList<>(List.of(firstEntry, secondEntry));

        when(raceRepository.findById(race.getRaceId())).thenReturn(Optional.of(race));
        when(raceEntryRepository.findByRace_RaceIdOrderByCreatedAtAsc(race.getRaceId()))
                .thenReturn(entries);
        doAnswer(invocation -> {
            Iterable<?> clearingEntries = invocation.getArgument(0);
            for (Object value : clearingEntries) {
                assertNull(((RaceEntry) value).getLaneNumber());
            }
            return entries;
        }).when(raceEntryRepository).saveAllAndFlush(any());

        raceEntryService.autoAssignLanes(race.getRaceId());

        Set<Integer> assignedLanes = new HashSet<>();
        assignedLanes.add(firstEntry.getLaneNumber());
        assignedLanes.add(secondEntry.getLaneNumber());
        assertEquals(Set.of(1, 2), assignedLanes);
        verify(raceEntryRepository).saveAllAndFlush(entries);
        verify(raceEntryRepository).saveAll(entries);
    }

    @Test
    void swapLanesTemporarilyReleasesLaneBeforeMovingBothEntries() {
        Round round = new Round();
        Race race = schedulingRace(round);
        RaceEntry firstEntry = raceEntry(race, 1);
        RaceEntry secondEntry = raceEntry(race, 2);
        AtomicInteger flushCall = new AtomicInteger();

        when(raceEntryRepository.findById(firstEntry.getEntryId()))
                .thenReturn(Optional.of(firstEntry));
        when(raceEntryRepository.findById(secondEntry.getEntryId()))
                .thenReturn(Optional.of(secondEntry));
        doAnswer(invocation -> {
            RaceEntry savedEntry = invocation.getArgument(0);
            int currentCall = flushCall.getAndIncrement();
            if (currentCall == 0) {
                assertSame(firstEntry, savedEntry);
                assertNull(savedEntry.getLaneNumber());
            } else {
                assertSame(secondEntry, savedEntry);
                assertEquals(1, savedEntry.getLaneNumber());
            }
            return savedEntry;
        }).when(raceEntryRepository).saveAndFlush(any(RaceEntry.class));

        raceEntryService.swapLanes(firstEntry.getEntryId(), secondEntry.getEntryId());

        assertEquals(2, firstEntry.getLaneNumber());
        assertEquals(1, secondEntry.getLaneNumber());
        assertEquals(2, flushCall.get());
        verify(raceEntryRepository, times(2)).saveAndFlush(any(RaceEntry.class));
        verify(raceEntryRepository).save(firstEntry);
    }

    private Race schedulingRace(Round round) {
        Race race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setRound(round);
        race.setStatus(RoundStatus.SCHEDULING);
        return race;
    }

    private RaceEntry raceEntry(Race race, Integer laneNumber) {
        RaceEntry entry = new RaceEntry();
        entry.setEntryId(UUID.randomUUID());
        entry.setRace(race);
        entry.setLaneNumber(laneNumber);
        return entry;
    }
}
