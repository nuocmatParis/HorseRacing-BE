package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.contract.request.InviteRequest;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.JockeyTournamentRegistration;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.ContractMapper;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.JockeyTournamentRegistrationRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceReportRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.impl.ContractServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractAvailabilityWorkflowTest {

    private JockeyHorseContractRepository contractRepository;
    private HorseTournamentRegistrationRepository horseRegistrationRepository;
    private JockeyTournamentRegistrationRepository jockeyRegistrationRepository;
    private UserCurrentService userCurrentService;
    private InvoiceService invoiceService;
    private ContractServiceImpl service;

    @BeforeEach
    void setUp() {
        contractRepository = mock(JockeyHorseContractRepository.class);
        horseRegistrationRepository = mock(HorseTournamentRegistrationRepository.class);
        jockeyRegistrationRepository = mock(JockeyTournamentRegistrationRepository.class);
        userCurrentService = mock(UserCurrentService.class);
        invoiceService = mock(InvoiceService.class);

        service = new ContractServiceImpl(
                contractRepository,
                horseRegistrationRepository,
                jockeyRegistrationRepository,
                mock(ContractMapper.class),
                userCurrentService,
                invoiceService,
                mock(InvoiceRepository.class),
                mock(PaymentService.class),
                mock(WalletRepository.class),
                mock(WalletTransactionRepository.class),
                mock(TournamentRepository.class),
                mock(RaceEntryRepository.class),
                mock(RaceRepository.class),
                mock(RaceReportRepository.class),
                mock(BusinessNotificationEventService.class)
        );
    }

    @Test
    void inviteMustRejectJockeyWhoAlreadyAcceptedAnotherHorseInTournament() {
        ContractScenario scenario = prepareInviteScenario();

        when(contractRepository
                .existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndStatusIn(
                        eq(scenario.jockeyRegistrationId), anyCollection()))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, new Executable() {
            @Override
            public void execute() {
                service.inviteJockey(scenario.request);
            }
        });

        assertEquals(ErrorCode.JOCKEY_ALREADY_CONTRACTED_IN_TOURNAMENT, exception.getErrorCode());
        verify(contractRepository, never()).save(org.mockito.ArgumentMatchers.any(JockeyHorseContract.class));
    }

    @Test
    void inviteMustRejectHorseThatAlreadyHasAcceptedJockeyInTournament() {
        ContractScenario scenario = prepareInviteScenario();

        when(contractRepository
                .existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndStatusIn(
                        eq(scenario.jockeyRegistrationId), anyCollection()))
                .thenReturn(false);
        when(contractRepository
                .existsByHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
                        eq(scenario.horseRegistrationId), anyCollection()))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, new Executable() {
            @Override
            public void execute() {
                service.inviteJockey(scenario.request);
            }
        });

        assertEquals(ErrorCode.CONTRACT_ALREADY_EXISTS_FOR_HORSE, exception.getErrorCode());
        verify(contractRepository, never()).save(org.mockito.ArgumentMatchers.any(JockeyHorseContract.class));
    }

    @Test
    void acceptMustRecheckJockeyAvailabilityBeforeChangingContractStatus() {
        UUID contractId = UUID.randomUUID();
        UUID horseRegistrationId = UUID.randomUUID();
        UUID jockeyRegistrationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User jockeyUser = User.builder().userId(userId).build();
        Jockey jockey = Jockey.builder().jockeyId(UUID.randomUUID()).user(jockeyUser).build();
        HorseTournamentRegistration horseRegistration = HorseTournamentRegistration.builder()
                .horseRegistrationId(horseRegistrationId)
                .status(RegistrationStatus.APPROVED)
                .build();
        JockeyTournamentRegistration jockeyRegistration = JockeyTournamentRegistration.builder()
                .jockeyTournamentRegId(jockeyRegistrationId)
                .status(RegistrationStatus.APPROVED)
                .build();
        JockeyHorseContract contract = JockeyHorseContract.builder()
                .contractId(contractId)
                .horseTournamentRegistration(horseRegistration)
                .jockeyTournamentRegistration(jockeyRegistration)
                .jockey(jockey)
                .status(ContractStatus.PENDING_JOCKEY)
                .build();

        when(userCurrentService.getCurrentUser()).thenReturn(jockeyUser);
        when(contractRepository.findForUpdateByContractId(contractId)).thenReturn(Optional.of(contract));
        when(horseRegistrationRepository.findForUpdateById(horseRegistrationId))
                .thenReturn(Optional.of(horseRegistration));
        when(jockeyRegistrationRepository.findForUpdateById(jockeyRegistrationId))
                .thenReturn(Optional.of(jockeyRegistration));
        when(contractRepository
                .existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndContractIdNotAndStatusIn(
                        eq(jockeyRegistrationId), eq(contractId), anyCollection()))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, new Executable() {
            @Override
            public void execute() {
                service.acceptContract(contractId);
            }
        });

        assertEquals(ErrorCode.JOCKEY_ALREADY_CONTRACTED_IN_TOURNAMENT, exception.getErrorCode());
        assertEquals(ContractStatus.PENDING_JOCKEY, contract.getStatus());
        verify(invoiceService, never()).createHiringFeeInvoice(
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.any(BigDecimal.class));
    }

    private ContractScenario prepareInviteScenario() {
        UUID tournamentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID horseRegistrationId = UUID.randomUUID();
        UUID jockeyRegistrationId = UUID.randomUUID();

        Tournament tournament = Tournament.builder()
                .tournamentId(tournamentId)
                .phase(TournamentPhase.JOCKEY_MATCHING)
                .build();
        HorseOwner owner = HorseOwner.builder().ownerId(ownerId).build();
        HorseTournamentRegistration horseRegistration = HorseTournamentRegistration.builder()
                .horseRegistrationId(horseRegistrationId)
                .tournament(tournament)
                .owner(owner)
                .status(RegistrationStatus.APPROVED)
                .build();
        JockeyTournamentRegistration jockeyRegistration = JockeyTournamentRegistration.builder()
                .jockeyTournamentRegId(jockeyRegistrationId)
                .tournament(tournament)
                .status(RegistrationStatus.APPROVED)
                .hireFee(new BigDecimal("1000000"))
                .build();
        InviteRequest request = InviteRequest.builder()
                .tournamentRegistrationId(horseRegistrationId)
                .jockeyTournamentRegistrationId(jockeyRegistrationId)
                .ownerPrizeSharePercent(70F)
                .jockeyPrizeSharePercent(30F)
                .build();

        when(userCurrentService.getCurrentOwner()).thenReturn(owner);
        when(horseRegistrationRepository.findForUpdateById(horseRegistrationId))
                .thenReturn(Optional.of(horseRegistration));
        when(jockeyRegistrationRepository.findForUpdateById(jockeyRegistrationId))
                .thenReturn(Optional.of(jockeyRegistration));
        when(contractRepository
                .existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
                        org.mockito.ArgumentMatchers.eq(jockeyRegistrationId),
                        org.mockito.ArgumentMatchers.eq(horseRegistrationId),
                        org.mockito.ArgumentMatchers.<Collection<ContractStatus>>any()))
                .thenReturn(false);

        return new ContractScenario(
                horseRegistrationId,
                jockeyRegistrationId,
                request
        );
    }

    private static class ContractScenario {
        private final UUID horseRegistrationId;
        private final UUID jockeyRegistrationId;
        private final InviteRequest request;

        private ContractScenario(UUID horseRegistrationId,
                                 UUID jockeyRegistrationId,
                                 InviteRequest request) {
            this.horseRegistrationId = horseRegistrationId;
            this.jockeyRegistrationId = jockeyRegistrationId;
            this.request = request;
        }
    }
}
