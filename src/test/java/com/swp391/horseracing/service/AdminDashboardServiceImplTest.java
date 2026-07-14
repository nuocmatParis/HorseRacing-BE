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
    void getSummaryCountsOnlyItemsThatNeedAdminAttention() {
        when(tournamentRepository.count()).thenReturn(7L);
        when(registrationRepository.countByStatus(RegistrationStatus.PENDING_REVIEW)).thenReturn(4L);
        when(contractRepository.countByStatus(ContractStatus.PENDING_ADMIN_REVIEW)).thenReturn(3L);
        when(raceRepository.countByStatus(RoundStatus.SCHEDULED)).thenReturn(9L);

        AdminDashboardSummaryResponse response = service.getSummary();

        assertEquals(7L, response.getTotalTournaments());
        assertEquals(4L, response.getPendingRegistrations());
        assertEquals(3L, response.getPendingContracts());
        assertEquals(9L, response.getScheduledRaces());
        assertNotNull(response.getGeneratedAt());

        verify(registrationRepository).countByStatus(RegistrationStatus.PENDING_REVIEW);
        verify(contractRepository).countByStatus(ContractStatus.PENDING_ADMIN_REVIEW);
        verify(raceRepository).countByStatus(RoundStatus.SCHEDULED);
    }
}
