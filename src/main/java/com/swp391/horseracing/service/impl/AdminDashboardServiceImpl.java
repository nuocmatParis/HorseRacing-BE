package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.admin.response.AdminDashboardSummaryResponse;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.AdminDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {
    TournamentRepository tournamentRepository;
    HorseTournamentRegistrationRepository horseRegistrationRepository;
    JockeyHorseContractRepository contractRepository;
    RaceRepository raceRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {
        return AdminDashboardSummaryResponse.builder()
                .totalTournaments(tournamentRepository.count())
                .pendingRegistrations(horseRegistrationRepository.countByStatus(RegistrationStatus.PENDING_REVIEW))
                .pendingContracts(0L)
                .activeContracts(contractRepository.countByStatus(ContractStatus.APPROVED))
                .scheduledRaces(raceRepository.countByStatus(RoundStatus.SCHEDULED))
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
