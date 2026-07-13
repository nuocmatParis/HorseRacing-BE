package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.request.UpdateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceEntryMapper;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.service.RaceEntryService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceEntryServiceImpl implements RaceEntryService {

    RaceEntryRepository raceEntryRepository;
    RaceRepository raceRepository;
    RoundRepository roundRepository;
    JockeyHorseContractRepository contractRepository;
    RaceEntryMapper raceEntryMapper;
    UserCurrentService userCurrentService;

    @Override
    @Transactional
    public RaceEntryResponse create(CreateRaceEntryRequest request) {
        Race race = raceRepository.findById(request.getRaceId())
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }

        if (race.getSchedulePublishedAt() != null) {
            throw new AppException(ErrorCode.RACE_ALREADY_PUBLISHED);
        }

        if (race.getStartedAt() != null) {
            throw new AppException(ErrorCode.RACE_ALREADY_STARTED);
        }

        JockeyHorseContract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() != ContractStatus.APPROVED) {
            throw new AppException(ErrorCode.CONTRACT_NOT_APPROVED);
        }

        if (contract.getHorseTournamentRegistration().getStatus() != RegistrationStatus.APPROVED) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_APPROVED);
        }

        if (!race.getRound().getTournament().getTournamentId()
                .equals(contract.getTournament().getTournamentId())) {
            throw new AppException(ErrorCode.TOURNAMENT_MISMATCH);
        }

        if (raceEntryRepository.existsByRace_RaceIdAndContract_ContractId(
                request.getRaceId(), request.getContractId())) {
            throw new AppException(ErrorCode.RACE_ENTRY_ALREADY_EXISTS);
        }

        if (request.getLaneNumber() != null) {
            if (request.getLaneNumber() > race.getRound().getMaxEntries()) {
                throw new AppException(ErrorCode.LANE_EXCEEDS_MAX);
            }
            if (raceEntryRepository.existsByRace_RaceIdAndLaneNumber(
                    request.getRaceId(), request.getLaneNumber())) {
                throw new AppException(ErrorCode.LANE_NUMBER_ALREADY_TAKEN);
            }
        }

        if (raceEntryRepository.countByRace_RaceId(request.getRaceId()) >= race.getRound().getMaxEntries()) {
            throw new AppException(ErrorCode.RACE_EXCEEDS_MAX_ENTRIES);
        }

        UUID roundId = race.getRound().getRoundId();
        List<RaceEntry> roundEntries = raceEntryRepository.findByRace_Round_RoundId(roundId);
        for (RaceEntry existing : roundEntries) {
            if (existing.getContract().getHorse().getHorseId()
                    .equals(contract.getHorse().getHorseId())) {
                throw new AppException(ErrorCode.HORSE_ALREADY_IN_ROUND);
            }
            if (existing.getContract().getJockey().getJockeyId()
                    .equals(contract.getJockey().getJockeyId())) {
                throw new AppException(ErrorCode.JOCKEY_ALREADY_IN_ROUND);
            }
        }

        User currentUser = userCurrentService.getCurrentUser();

        RaceEntry raceEntry = raceEntryMapper.toRaceEntry(request);
        raceEntry.setRace(race);
        raceEntry.setContract(contract);
        raceEntry.setAssignedBy(currentUser);
        raceEntry.setAssignedAt(LocalDateTime.now());
        raceEntry.setStatus(RaceEntryStatus.CONFIRMED);

        return raceEntryMapper.toRaceEntryResponse(raceEntryRepository.save(raceEntry));
    }

    @Override
    @Transactional
    public RaceEntryResponse updateStatus(UUID entryId, UpdateRaceEntryRequest request) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        if (request.getStatus() == null) {
            throw new AppException(ErrorCode.INVALID_RACE_ENTRY_STATUS);
        }

        RaceEntryStatus currentStatus = raceEntry.getStatus();
        RaceEntryStatus newStatus = request.getStatus();

        if (currentStatus == newStatus) {
            return raceEntryMapper.toRaceEntryResponse(raceEntry);
        }

        switch (currentStatus) {
            case CONFIRMED -> {
                if (newStatus == RaceEntryStatus.SCRATCHED) {
                    raceEntry.setStatus(RaceEntryStatus.SCRATCHED);
                    raceEntry.setScratchedReason(request.getScratchedReason());
                } else if (newStatus == RaceEntryStatus.DISQUALIFIED) {
                    raceEntry.setStatus(RaceEntryStatus.DISQUALIFIED);
                    raceEntry.setDisqualifiedAt(LocalDateTime.now());
                } else if (newStatus == RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE) {
                    raceEntry.setStatus(RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE);
                    raceEntry.setWithdrawnAt(LocalDateTime.now());
                    raceEntry.setWithdrawReason(request.getWithdrawReason());
                } else if (newStatus == RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE) {
                    raceEntry.setStatus(RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE);
                    raceEntry.setWithdrawnAt(LocalDateTime.now());
                    raceEntry.setWithdrawReason(request.getWithdrawReason());
                } else {
                    throw new AppException(ErrorCode.INVALID_RACE_ENTRY_STATUS_TRANSITION);
                }
            }
            case SCRATCHED -> {
                if (newStatus != RaceEntryStatus.DISQUALIFIED) {
                    throw new AppException(ErrorCode.INVALID_RACE_ENTRY_STATUS_TRANSITION);
                }
                raceEntry.setStatus(RaceEntryStatus.DISQUALIFIED);
                raceEntry.setDisqualifiedAt(LocalDateTime.now());
            }
            case WITHDRAWN_BEFORE_SCHEDULE -> {
                throw new AppException(ErrorCode.INVALID_RACE_ENTRY_STATUS_TRANSITION);
            }
            case WITHDRAWN_AFTER_SCHEDULE -> {
                throw new AppException(ErrorCode.INVALID_RACE_ENTRY_STATUS_TRANSITION);
            }
            case DISQUALIFIED -> {
                throw new AppException(ErrorCode.INVALID_RACE_ENTRY_STATUS_TRANSITION);
            }
            case FINISHED -> {
                throw new AppException(ErrorCode.INVALID_RACE_ENTRY_STATUS_TRANSITION);
            }
        }

        return raceEntryMapper.toRaceEntryResponse(raceEntryRepository.save(raceEntry));
    }

    @Override
    public List<RaceEntryResponse> getEntriesByRaceId(UUID raceId) {
        return raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId)
                .stream()
                .map(raceEntryMapper::toRaceEntryResponse)
                .toList();
    }

    @Override
    public RaceEntryResponse getEntryById(UUID entryId) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
        return raceEntryMapper.toRaceEntryResponse(raceEntry);
    }

    @Override
    @Transactional
    public void delete(UUID entryId) {
        RaceEntry raceEntry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        Race race = raceEntry.getRace();
        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }
        if (race.getSchedulePublishedAt() != null) {
            throw new AppException(ErrorCode.RACE_ALREADY_PUBLISHED);
        }

        raceEntryRepository.delete(raceEntry);
    }

    @Override
    @Transactional
    public void autoAssignRound(UUID roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new AppException(ErrorCode.ROUND_NOT_FOUND));

        if (round.getTournament().getPhase() != TournamentPhase.SCHEDULING) {
            throw new AppException(ErrorCode.INVALID_PHASE_TRANSITION);
        }

        List<Race> races = raceRepository.findByRound_RoundId(roundId);
        if (races.isEmpty()) {
            throw new AppException(ErrorCode.ROUND_MISSING_RACES);
        }

        List<JockeyHorseContract> allContracts = contractRepository
                .findByTournament_TournamentIdAndStatus(
                        round.getTournament().getTournamentId(), ContractStatus.APPROVED);

        List<UUID> assignedContractIds = raceEntryRepository.findByRace_Round_RoundId(roundId)
                .stream()
                .map(e -> e.getContract().getContractId())
                .toList();

        List<JockeyHorseContract> available = new ArrayList<>();
        for (JockeyHorseContract c : allContracts) {
            if (!assignedContractIds.contains(c.getContractId())) {
                available.add(c);
            }
        }

        if (available.isEmpty()) {
            return;
        }

        Collections.shuffle(available);

        int total = available.size();
        int raceCount = races.size();
        int base = total / raceCount;
        int remainder = total % raceCount;

        int contractIndex = 0;
        User currentUser = userCurrentService.getCurrentUser();
        List<RaceEntry> allNewEntries = new ArrayList<>();

        for (int i = 0; i < raceCount; i++) {
            int countForThisRace = base + (i < remainder ? 1 : 0);
            List<JockeyHorseContract> raceContracts = new ArrayList<>();
            for (int j = 0; j < countForThisRace && contractIndex < total; j++) {
                raceContracts.add(available.get(contractIndex++));
            }

            Collections.shuffle(raceContracts);

            Race race = races.get(i);
            for (int lane = 0; lane < raceContracts.size(); lane++) {
                RaceEntry entry = RaceEntry.builder()
                        .race(race)
                        .contract(raceContracts.get(lane))
                        .laneNumber(lane + 1)
                        .status(RaceEntryStatus.CONFIRMED)
                        .assignedBy(currentUser)
                        .assignedAt(LocalDateTime.now())
                        .build();
                allNewEntries.add(entry);
            }
        }

        raceEntryRepository.saveAll(allNewEntries);
    }

    @Override
    @Transactional
    public void autoAssignLanes(UUID raceId) {
        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));

        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }

        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByCreatedAtAsc(raceId);
        if (entries.isEmpty()) {
            return;
        }

        Collections.shuffle(entries);

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setLaneNumber(i + 1);
        }

        raceEntryRepository.saveAll(entries);
    }

    @Override
    @Transactional
    public RaceEntryResponse updateLane(UUID entryId, Integer laneNumber) {
        RaceEntry entry = raceEntryRepository.findById(entryId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        Race race = entry.getRace();
        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }

        if (laneNumber != null) {
            if (laneNumber > race.getRound().getMaxEntries()) {
                throw new AppException(ErrorCode.LANE_EXCEEDS_MAX);
            }

            RaceEntry conflicting = raceEntryRepository.findByRace_RaceIdOrderByCreatedAtAsc(race.getRaceId())
                    .stream()
                    .filter(e -> !e.getEntryId().equals(entryId)
                            && laneNumber.equals(e.getLaneNumber()))
                    .findFirst()
                    .orElse(null);

            if (conflicting != null) {
                throw new AppException(ErrorCode.LANE_NUMBER_ALREADY_TAKEN);
            }
        }

        entry.setLaneNumber(laneNumber);
        return raceEntryMapper.toRaceEntryResponse(raceEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public RaceEntryResponse swapLanes(UUID entryId1, UUID entryId2) {
        if (entryId1.equals(entryId2)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        RaceEntry entry1 = raceEntryRepository.findById(entryId1)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));
        RaceEntry entry2 = raceEntryRepository.findById(entryId2)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_ENTRY_NOT_FOUND));

        if (!entry1.getRace().getRaceId().equals(entry2.getRace().getRaceId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Race race = entry1.getRace();
        if (race.getStatus() != RoundStatus.SCHEDULING) {
            throw new AppException(ErrorCode.RACE_NOT_IN_SCHEDULING);
        }

        Integer tempLane = entry1.getLaneNumber();
        entry1.setLaneNumber(entry2.getLaneNumber());
        entry2.setLaneNumber(tempLane);

        raceEntryRepository.save(entry1);
        raceEntryRepository.save(entry2);

        return raceEntryMapper.toRaceEntryResponse(entry1);
    }
}
