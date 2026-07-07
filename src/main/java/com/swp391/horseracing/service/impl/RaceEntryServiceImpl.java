package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.request.UpdateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.RaceEntryMapper;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.service.RaceEntryService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RaceEntryServiceImpl implements RaceEntryService {

    RaceEntryRepository raceEntryRepository;
    RaceRepository raceRepository;
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

        if (request.getLaneNumber() > race.getRound().getMaxEntries()) {
            throw new AppException(ErrorCode.LANE_EXCEEDS_MAX);
        }

        if (raceEntryRepository.existsByRace_RaceIdAndLaneNumber(
                request.getRaceId(), request.getLaneNumber())) {
            throw new AppException(ErrorCode.LANE_NUMBER_ALREADY_TAKEN);
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
}
