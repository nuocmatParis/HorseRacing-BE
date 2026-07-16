package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.jockey.response.JockeyAssignedHorseResponse;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.service.JockeyAssignedHorseService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JockeyAssignedHorseServiceImpl implements JockeyAssignedHorseService {
    UserCurrentService userCurrentService;
    JockeyHorseContractRepository contractRepository;
    RaceEntryRepository raceEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JockeyAssignedHorseResponse> getMyAssignedHorses() {
        User currentUser = userCurrentService.getCurrentUser();
        List<JockeyHorseContract> contracts = contractRepository
                .findByJockey_User_UserIdAndStatusOrderByRequestedAtDesc(currentUser.getUserId(), ContractStatus.APPROVED);
        LocalDateTime now = LocalDateTime.now();
        List<JockeyAssignedHorseResponse> responses = new ArrayList<>();
        for (JockeyHorseContract contract : contracts) {
            RaceEntry nextEntry = findNextEntry(contract, now);
            responses.add(JockeyAssignedHorseResponse.builder()
                    .horseId(contract.getHorse().getHorseId())
                    .horseName(contract.getHorse().getName())
                    .imageUrl(contract.getHorse().getImageUrl())
                    .ownerId(contract.getOwner().getOwnerId())
                    .ownerName(contract.getOwner().getUser().getFullName())
                    .tournamentId(contract.getTournament().getTournamentId())
                    .tournamentName(contract.getTournament().getName())
                    .contractId(contract.getContractId())
                    .contractStatus(contract.getStatus())
                    .currentRating(contract.getHorse().getCurrentRating())
                    .raceClass(contract.getHorse().getRaceClass())
                    .healthStatus(contract.getHorse().getHealthStatus())
                    .totalRaces(contract.getHorse().getTotalRaces())
                    .totalWins(contract.getHorse().getTotalWins())
                    .totalTop3Finishes(contract.getHorse().getTotalTop3Finishes())
                    .winRate(contract.getHorse().getWinRate())
                    .nextRaceId(nextEntry == null ? null : nextEntry.getRace().getRaceId())
                    .nextRaceName(nextEntry == null ? null : nextEntry.getRace().getName())
                    .nextRaceStartTime(nextEntry == null ? null : nextEntry.getRace().getStartTime())
                    .laneNumber(nextEntry == null ? null : nextEntry.getLaneNumber())
                    .build());
        }
        return responses;
    }

    private RaceEntry findNextEntry(JockeyHorseContract contract, LocalDateTime now) {
        List<RaceEntry> entries = raceEntryRepository.findByContract_ContractId(contract.getContractId());
        RaceEntry next = null;
        for (RaceEntry entry : entries) {
            if (entry.getRace().getStartTime() == null || entry.getRace().getStartTime().isBefore(now)) {
                continue;
            }
            if (next == null || entry.getRace().getStartTime().isBefore(next.getRace().getStartTime())) {
                next = entry;
            }
        }
        return next;
    }
}
