package com.swp391.horseracing.service;

import com.swp391.horseracing.config.HorseRatingProperties;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.dto.tournament.request.TournamentRatingConfigRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.PrizeStructure;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.TournamentEligibility;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.AdvancePayoutStatus;
import com.swp391.horseracing.enums.ContractPaymentStatus;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.EscrowStatus;
import com.swp391.horseracing.enums.FinalPayoutStatus;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.InvoiceType;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.ContractMapper;
import com.swp391.horseracing.mapper.HorseTournamentRegistrationMapper;
import com.swp391.horseracing.mapper.JockeyTournamentRegistrationMapper;
import com.swp391.horseracing.mapper.TournamentMapper;
import com.swp391.horseracing.repository.HorseOwnerRepository;
import com.swp391.horseracing.repository.HorseRepository;
import com.swp391.horseracing.repository.HorseTournamentRegistrationRepository;
import com.swp391.horseracing.repository.InvoiceRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.JockeyRepository;
import com.swp391.horseracing.repository.JockeyTournamentRegistrationRepository;
import com.swp391.horseracing.repository.PrizeStructureRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRefereeRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.repository.RoundRepository;
import com.swp391.horseracing.repository.TournamentEligibilityRepository;
import com.swp391.horseracing.repository.PhaseTimingConfigRepository;
import com.swp391.horseracing.repository.TournamentPhaseConfigRepository;
import com.swp391.horseracing.repository.TournamentRepository;
import com.swp391.horseracing.repository.UserRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.impl.ContractServiceImpl;
import com.swp391.horseracing.service.impl.TournamentRegistrationServiceImpl;
import com.swp391.horseracing.service.impl.TournamentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AdditionalApiBusinessLogicTest {
    @Mock JockeyHorseContractRepository contractRepository;
    @Mock HorseTournamentRegistrationRepository horseRegistrationRepository;
    @Mock JockeyTournamentRegistrationRepository jockeyRegistrationRepository;
    @Mock ContractMapper contractMapper;
    @Mock UserCurrentService userCurrentService;
    @Mock InvoiceService invoiceService;
    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentService paymentService;
    @Mock WalletRepository walletRepository;
    @Mock WalletTransactionRepository walletTransactionRepository;
    @Mock TournamentRepository tournamentRepository;
    @Mock RaceEntryRepository raceEntryRepository;
    @Mock BusinessNotificationEventService notificationEventService;

    @Mock HorseRepository horseRepository;
    @Mock HorseOwnerRepository horseOwnerRepository;
    @Mock JockeyRepository jockeyRepository;
    @Mock HorseTournamentRegistrationMapper horseRegistrationMapper;
    @Mock JockeyTournamentRegistrationMapper jockeyRegistrationMapper;

    @Mock UserRepository userRepository;
    @Mock TournamentMapper tournamentMapper;
    @Mock PrizeStructureRepository prizeStructureRepository;
    @Mock TournamentEligibilityRepository eligibilityRepository;
    @Mock RoundRepository roundRepository;
    @Mock RaceRepository raceRepository;
    @Mock RaceRefereeRepository raceRefereeRepository;
    @Mock RaceService raceService;
    @Mock PhaseTimingConfigRepository phaseTimingConfigRepository;
    @Mock TournamentPhaseConfigRepository tournamentPhaseConfigRepository;
    @Mock HorseRatingProperties horseRatingProperties;
    @Mock CloudinaryService cloudinaryService;

    @InjectMocks ContractServiceImpl contractService;
    @InjectMocks TournamentRegistrationServiceImpl registrationService;
    @InjectMocks TournamentServiceImpl tournamentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancelApprovedContractRefundsOnlyRemainingEscrow() {
        UUID contractId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User ownerUser = new User();
        ownerUser.setUserId(userId);
        HorseOwner owner = new HorseOwner();
        owner.setUser(ownerUser);
        Tournament tournament = new Tournament();
        tournament.setPhase(TournamentPhase.SCHEDULING);

        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setContractId(contractId);
        contract.setOwner(owner);
        contract.setTournament(tournament);
        contract.setStatus(ContractStatus.APPROVED);
        contract.setPaymentStatus(ContractPaymentStatus.PAID);
        contract.setEscrowStatus(EscrowStatus.PARTIALLY_RELEASED);
        contract.setEscrowAmount(new BigDecimal("70.00"));
        contract.setAdvancePayoutStatus(AdvancePayoutStatus.PAID);
        contract.setFinalPayoutStatus(FinalPayoutStatus.NOT_RELEASED);

        Invoice hiringInvoice = new Invoice();
        hiringInvoice.setInvoiceId(invoiceId);
        hiringInvoice.setInvoiceType(InvoiceType.JOCKEY_HIRING_FEE);
        hiringInvoice.setStatus(InvoiceStatus.PAID);
        hiringInvoice.setAmount(new BigDecimal("100.00"));

        when(userCurrentService.getCurrentUser()).thenReturn(ownerUser);
        when(contractRepository.findForUpdateByContractId(contractId)).thenReturn(Optional.of(contract));
        when(raceEntryRepository.findByContract_ContractId(contractId)).thenReturn(Collections.emptyList());
        when(invoiceRepository.findByContractIdAndInvoiceType(contractId, InvoiceType.JOCKEY_HIRING_FEE))
                .thenReturn(Optional.of(hiringInvoice));
        when(invoiceRepository.findByContractIdAndInvoiceType(contractId, InvoiceType.CONTRACT_CREATION_FEE))
                .thenReturn(Optional.empty());
        when(contractRepository.save(contract)).thenReturn(contract);
        when(contractMapper.toContractResponse(contract)).thenReturn(new ContractResponse());

        contractService.cancelByOwner(contractId, "Owner withdraws");

        verify(paymentService).refundInvoiceAmount(invoiceId, new BigDecimal("70.00"));
        verify(paymentService, never()).refundInvoice(invoiceId);
        assertEquals(ContractStatus.CANCELLED, contract.getStatus());
        assertEquals(ContractPaymentStatus.PARTIALLY_REFUNDED, contract.getPaymentStatus());
        assertEquals(BigDecimal.ZERO, contract.getEscrowAmount());
        assertEquals(FinalPayoutStatus.CANCELLED, contract.getFinalPayoutStatus());
    }

    @Test
    void withdrawCancelsPendingInvitationsButDoesNotRefundPaidRegistrationFee() {
        UUID registrationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User ownerUser = new User();
        ownerUser.setUserId(userId);
        HorseOwner owner = new HorseOwner();
        owner.setUser(ownerUser);
        Tournament tournament = new Tournament();
        tournament.setPhase(TournamentPhase.SCHEDULING);

        HorseTournamentRegistration registration = new HorseTournamentRegistration();
        registration.setHorseRegistrationId(registrationId);
        registration.setOwner(owner);
        registration.setTournament(tournament);
        registration.setStatus(RegistrationStatus.APPROVED);

        JockeyHorseContract invitation = new JockeyHorseContract();
        invitation.setStatus(ContractStatus.PENDING_JOCKEY);
        invitation.setAdvancePayoutStatus(AdvancePayoutStatus.NOT_PAID);
        invitation.setFinalPayoutStatus(FinalPayoutStatus.NOT_RELEASED);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PAID);

        when(horseRegistrationRepository.findForUpdateById(registrationId)).thenReturn(Optional.of(registration));
        when(userCurrentService.getCurrentUser()).thenReturn(ownerUser);
        when(contractRepository.findByHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
                registrationId, List.of(ContractStatus.ACCEPTED, ContractStatus.HIRING_PAID,
                        ContractStatus.PENDING_ADMIN_REVIEW, ContractStatus.APPROVED)))
                .thenReturn(Collections.emptyList());
        when(contractRepository.findByHorseTournamentRegistration_HorseRegistrationIdAndStatus(
                registrationId, ContractStatus.PENDING_JOCKEY)).thenReturn(List.of(invitation));
        when(invoiceRepository.findByHorseTournamentRegistration_HorseRegistrationId(registrationId))
                .thenReturn(Optional.of(invoice));
        when(contractRepository.save(org.mockito.ArgumentMatchers.any(JockeyHorseContract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(horseRegistrationRepository.save(registration)).thenReturn(registration);
        when(horseRegistrationMapper.toHorseTournamentRegistrationResponse(registration))
                .thenReturn(new HorseTournamentRegistrationResponse());

        registrationService.withdrawHorseRegistration(registrationId, "Horse unavailable");

        assertEquals(RegistrationStatus.WITHDRAWN, registration.getStatus());
        assertEquals(ContractStatus.CANCELLED, invitation.getStatus());
        verify(invoiceService, never()).cancelInvoice(org.mockito.ArgumentMatchers.any());
        verify(paymentService, never()).refundInvoice(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelContractIsBlockedWhenItsRaceWasAlreadyPublished() {
        UUID contractId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User ownerUser = new User();
        ownerUser.setUserId(userId);
        HorseOwner owner = new HorseOwner();
        owner.setUser(ownerUser);
        Tournament tournament = new Tournament();
        tournament.setPhase(TournamentPhase.SCHEDULING);
        JockeyHorseContract contract = new JockeyHorseContract();
        contract.setContractId(contractId);
        contract.setOwner(owner);
        contract.setTournament(tournament);
        contract.setStatus(ContractStatus.APPROVED);
        Race race = new Race();
        race.setStatus(RoundStatus.SCHEDULED);
        RaceEntry entry = new RaceEntry();
        entry.setRace(race);

        when(userCurrentService.getCurrentUser()).thenReturn(ownerUser);
        when(contractRepository.findForUpdateByContractId(contractId)).thenReturn(Optional.of(contract));
        when(raceEntryRepository.findByContract_ContractId(contractId)).thenReturn(List.of(entry));

        assertThrows(com.swp391.horseracing.exception.AppException.class,
                () -> contractService.cancelByOwner(contractId, "Too late"));
        verify(paymentService, never()).refundInvoiceAmount(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void withdrawIsBlockedUntilAcceptedContractIsCancelled() {
        UUID registrationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User ownerUser = new User();
        ownerUser.setUserId(userId);
        HorseOwner owner = new HorseOwner();
        owner.setUser(ownerUser);
        Tournament tournament = new Tournament();
        tournament.setPhase(TournamentPhase.SCHEDULING);
        HorseTournamentRegistration registration = new HorseTournamentRegistration();
        registration.setHorseRegistrationId(registrationId);
        registration.setOwner(owner);
        registration.setTournament(tournament);
        registration.setStatus(RegistrationStatus.APPROVED);
        JockeyHorseContract acceptedContract = new JockeyHorseContract();
        acceptedContract.setStatus(ContractStatus.ACCEPTED);

        when(horseRegistrationRepository.findForUpdateById(registrationId)).thenReturn(Optional.of(registration));
        when(userCurrentService.getCurrentUser()).thenReturn(ownerUser);
        when(contractRepository.findByHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
                registrationId, List.of(ContractStatus.ACCEPTED, ContractStatus.HIRING_PAID,
                        ContractStatus.PENDING_ADMIN_REVIEW, ContractStatus.APPROVED)))
                .thenReturn(List.of(acceptedContract));

        assertThrows(com.swp391.horseracing.exception.AppException.class,
                () -> registrationService.withdrawHorseRegistration(registrationId, "Withdraw"));
        assertEquals(RegistrationStatus.APPROVED, registration.getStatus());
    }

    @Test
    void closeRegistrationRejectsOnlyUnpaidRegistrationsAndCancelsTheirInvoice() {
        UUID tournamentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setPhase(TournamentPhase.REGISTRATION_OPEN);
        User admin = new User();
        admin.setUsername("admin");
        HorseTournamentRegistration unpaid = new HorseTournamentRegistration();
        unpaid.setStatus(RegistrationStatus.PENDING_PAYMENT);
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setStatus(InvoiceStatus.UNPAID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password"));
        when(tournamentRepository.findForUpdateByTournamentId(tournamentId)).thenReturn(Optional.of(tournament));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(horseRegistrationRepository.findForUpdateByTournamentIdAndStatus(
                tournamentId, RegistrationStatus.PENDING_PAYMENT)).thenReturn(List.of(unpaid));
        when(invoiceRepository.findByHorseTournamentRegistration_HorseRegistrationId(unpaid.getHorseRegistrationId()))
                .thenReturn(Optional.of(invoice));
        when(tournamentRepository.save(tournament)).thenReturn(tournament);
        when(tournamentMapper.toTournamentResponse(tournament)).thenReturn(new TournamentResponse());

        TournamentResponse response = tournamentService.closeRegistration(tournamentId);

        assertNotNull(response);
        assertEquals(TournamentPhase.REGISTRATION_REVIEW, tournament.getPhase());
        assertEquals(RegistrationStatus.REJECTED, unpaid.getStatus());
        verify(invoiceService).cancelInvoice(invoiceId);
    }

    @Test
    void updatesTournamentRatingConfigOnlyWhileDraftAndIncrementsVersion() {
        UUID tournamentId = UUID.randomUUID();
        LocalDateTime registrationOpenAt = LocalDateTime.now().plusDays(1);
        LocalDateTime registrationCloseAt = registrationOpenAt.plusDays(3);
        LocalDateTime reviewDeadlineAt = registrationCloseAt.plusDays(4);
        LocalDateTime matchingDeadlineAt = reviewDeadlineAt.plusDays(3);
        LocalDateTime schedulingDeadlineAt = matchingDeadlineAt.plusDays(4);

        Tournament tournament = Tournament.builder()
                .tournamentId(tournamentId)
                .name("Rating Config Tournament")
                .status(TournamentStatus.DRAFT)
                .phase(TournamentPhase.DRAFT)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .minHorseAge(3)
                .maxHorseAge(12)
                .maxApprovedEntries(8)
                .registrationOpenAt(registrationOpenAt)
                .registrationCloseAt(registrationCloseAt)
                .reviewDeadlineAt(reviewDeadlineAt)
                .jockeyMatchingDeadlineAt(matchingDeadlineAt)
                .schedulingDeadlineAt(schedulingDeadlineAt)
                .ratingPolicyVersion(1)
                .build();

        TournamentRatingConfigRequest ratingConfig =
                TournamentRatingConfigRequest.builder()
                        .firstMin(20)
                        .firstMax(25)
                        .disqualifiedMin(-12)
                        .disqualifiedMax(-4)
                        .build();
        UpdateTournamentRequest request = UpdateTournamentRequest.builder()
                .ratingConfig(ratingConfig)
                .build();

        when(tournamentRepository.findById(tournamentId))
                .thenReturn(Optional.of(tournament));
        when(tournamentRepository.save(tournament)).thenReturn(tournament);
        when(tournamentMapper.toTournamentResponse(tournament))
                .thenReturn(new TournamentResponse());

        TournamentResponse response = tournamentService.update(tournamentId, request);

        assertEquals(20, tournament.getRatingFirstMin());
        assertEquals(25, tournament.getRatingFirstMax());
        assertEquals(-12, tournament.getRatingDisqualifiedMin());
        assertEquals(-4, tournament.getRatingDisqualifiedMax());
        assertEquals(2, tournament.getRatingPolicyVersion());
        assertNotNull(response.getRatingConfig());
        assertEquals(20, response.getRatingConfig().getFirstMin());
    }

    @Test
    void publishingTournamentLocksItsRatingConfig() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .tournamentId(tournamentId)
                .status(TournamentStatus.DRAFT)
                .phase(TournamentPhase.DRAFT)
                .ratingPolicyVersion(1)
                .build();
        PrizeStructure prizeStructure = new PrizeStructure();
        TournamentEligibility eligibility = new TournamentEligibility();

        when(tournamentRepository.findById(tournamentId))
                .thenReturn(Optional.of(tournament));
        when(prizeStructureRepository.findByTournament_TournamentId(tournamentId))
                .thenReturn(List.of(prizeStructure));
        when(eligibilityRepository.findByTournament_TournamentId(tournamentId))
                .thenReturn(List.of(eligibility));
        when(tournamentRepository.save(tournament)).thenReturn(tournament);
        when(tournamentMapper.toTournamentResponse(tournament))
                .thenReturn(new TournamentResponse());

        TournamentResponse response = tournamentService.publish(tournamentId);

        assertNotNull(tournament.getRatingPolicyLockedAt());
        assertEquals(TournamentPhase.REGISTRATION_OPEN, tournament.getPhase());
        assertEquals(TournamentStatus.OPEN, tournament.getStatus());
        assertNotNull(response.getRatingConfig());
        assertEquals(true, response.getRatingConfig().isLocked());
    }

}
