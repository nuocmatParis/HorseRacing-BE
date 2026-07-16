package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.admin.response.AdminDashboardSummaryResponse;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.service.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardServiceImplTest {

    private TournamentRepository tournamentRepository;
    private HorseTournamentRegistrationRepository registrationRepository;
    private JockeyHorseContractRepository contractRepository;
    private RaceRepository raceRepository;
    private AdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        tournamentRepository = mock(TournamentRepository.class);
        registrationRepository = mock(HorseTournamentRegistrationRepository.class);
        contractRepository = mock(JockeyHorseContractRepository.class);
        raceRepository = mock(RaceRepository.class);
        service = new AdminDashboardServiceImpl(
                tournamentRepository,
                registrationRepository,
                contractRepository,
                raceRepository);
    }

    @Test
    void getSummaryCountsActiveContractsWithoutAnAdminReviewQueue() {
        when(tournamentRepository.count()).thenReturn(7L);
        when(registrationRepository.countByStatus(RegistrationStatus.PENDING_REVIEW)).thenReturn(4L);
        when(contractRepository.countByStatus(ContractStatus.APPROVED)).thenReturn(3L);
        when(raceRepository.countByStatus(RoundStatus.SCHEDULED)).thenReturn(9L);

        AdminDashboardSummaryResponse response = service.getSummary();

        assertEquals(7L, response.getTotalTournaments());
        assertEquals(4L, response.getPendingRegistrations());
        assertEquals(0L, response.getPendingContracts());
        assertEquals(3L, response.getActiveContracts());
        assertEquals(9L, response.getScheduledRaces());
        assertNotNull(response.getGeneratedAt());

        verify(registrationRepository).countByStatus(RegistrationStatus.PENDING_REVIEW);
        verify(contractRepository).countByStatus(ContractStatus.APPROVED);
        verify(raceRepository).countByStatus(RoundStatus.SCHEDULED);
    }
}
