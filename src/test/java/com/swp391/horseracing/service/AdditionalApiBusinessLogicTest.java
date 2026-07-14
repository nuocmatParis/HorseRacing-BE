package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.dto.tournament.response.BracketPreviewResponse;
import com.swp391.horseracing.dto.tournament.response.RoundPreviewDto;
import com.swp391.horseracing.dto.tournament.response.TournamentScheduleProposalResponse;
import com.swp391.horseracing.dto.tournament.request.ConfirmBracketRequest;
import com.swp391.horseracing.enums.BracketPlanStatus;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.Tournament;
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
    void getBracketPreviewDraftPhaseValidatesPowerOf2AndCalculatesRounds() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setMaxApprovedEntries(64);
        tournament.setStartDate(java.time.LocalDate.now());
        tournament.setEndDate(java.time.LocalDate.now().plusDays(30));

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(contractRepository.countByTournament_TournamentIdAndStatus(tournamentId, ContractStatus.APPROVED)).thenReturn(0L);

        BracketPreviewResponse response = tournamentService.getBracketPreview(tournamentId);

        assertNotNull(response);
        assertEquals(64, response.getMaxApprovedEntries());
        assertEquals(0, response.getActualApprovedEntries());
        assertEquals(3, response.getRounds().size());
        assertEquals(4, response.getRounds().get(0).getRaceCount());
        assertEquals(List.of(16, 16, 16, 16), response.getRounds().get(0).getEntriesPerRace());
        assertEquals(2, response.getRounds().get(1).getRaceCount());
        assertEquals(List.of(8, 8), response.getRounds().get(1).getEntriesPerRace());
        assertEquals(1, response.getRounds().get(2).getRaceCount());
        assertEquals(List.of(8), response.getRounds().get(2).getEntriesPerRace());
        assertEquals(7, response.getTotalRaceCount());
        assertEquals(true, response.isValid());
    }

    @Test
    void getBracketPreviewStaleIfActualApprovedEntriesLessThanMinimum() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setMaxApprovedEntries(64);
        tournament.setStartDate(java.time.LocalDate.now());
        tournament.setEndDate(java.time.LocalDate.now().plusDays(30));

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(contractRepository.countByTournament_TournamentIdAndStatus(tournamentId, ContractStatus.APPROVED)).thenReturn(30L);

        BracketPreviewResponse response = tournamentService.getBracketPreview(tournamentId);

        assertNotNull(response);
        assertEquals(false, response.isValid());
        assertEquals(32, response.getRecommendedMaxApprovedEntries());
    }

    @Test
    void getBracketPreviewDistributesFiftyEntriesAsThirteenThirteenTwelveTwelve() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setMaxApprovedEntries(64);
        tournament.setStartDate(LocalDate.now());
        tournament.setEndDate(LocalDate.now().plusDays(30));

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(contractRepository.countByTournament_TournamentIdAndStatus(
                tournamentId, ContractStatus.APPROVED)).thenReturn(50L);

        BracketPreviewResponse response = tournamentService.getBracketPreview(tournamentId);

        assertEquals(true, response.isValid());
        assertEquals(List.of(13, 13, 12, 12), response.getRounds().get(0).getEntriesPerRace());
    }

    @Test
    void getBracketPreviewRejectsNonPowerOfTwo() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setMaxApprovedEntries(24);
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        AppException exception = assertThrows(AppException.class,
                () -> tournamentService.getBracketPreview(tournamentId));

        assertEquals(ErrorCode.INVALID_MAX_APPROVED_ENTRIES, exception.getErrorCode());
    }

    @Test
    void confirmBracketCreatesOnlyRoundAndRaceSkeletons() {
        UUID tournamentId = UUID.randomUUID();
        User admin = new User();
        admin.setUserId(UUID.randomUUID());
        admin.setUsername("admin");

        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setName("Skeleton Cup");
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPhase(TournamentPhase.DRAFT);
        tournament.setStartDate(LocalDate.now());
        tournament.setEndDate(LocalDate.now().plusDays(30));
        tournament.setMaxApprovedEntries(64);
        tournament.setMaxApprovedHorses(64);
        tournament.setMaxApprovedJockeys(80);
        tournament.setBracketPlanStatus(BracketPlanStatus.NOT_GENERATED);
        tournament.setBracketPlanVersion(1);
        tournament.setCreatedBy(admin);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password"));
        when(tournamentRepository.findForUpdateByTournamentId(tournamentId)).thenReturn(Optional.of(tournament));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(contractRepository.countByTournament_TournamentIdAndStatus(
                tournamentId, ContractStatus.APPROVED)).thenReturn(0L);
        when(roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId))
                .thenReturn(Collections.emptyList());
        when(roundRepository.save(any(com.swp391.horseracing.entity.Round.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(tournamentRepository.save(tournament)).thenReturn(tournament);
        when(tournamentMapper.toTournamentResponse(tournament)).thenReturn(new TournamentResponse());

        tournamentService.confirmBracket(tournamentId, new ConfirmBracketRequest(64, 1));

        assertEquals(BracketPlanStatus.CONFIRMED, tournament.getBracketPlanStatus());
        assertEquals(2, tournament.getBracketPlanVersion());
        assertEquals(3, tournament.getPlannedRoundCount());
        assertEquals(7, tournament.getPlannedRaceCount());
        verify(roundRepository, times(3)).save(any(com.swp391.horseracing.entity.Round.class));
        verify(raceRepository, times(7)).save(any(Race.class));
    }

    @Test
    void confirmBracketRejectsOutdatedPlanVersionBeforeChangingStructure() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setBracketPlanStatus(BracketPlanStatus.NOT_GENERATED);
        tournament.setBracketPlanVersion(3);
        when(tournamentRepository.findForUpdateByTournamentId(tournamentId)).thenReturn(Optional.of(tournament));

        AppException exception = assertThrows(AppException.class,
                () -> tournamentService.confirmBracket(
                        tournamentId, new ConfirmBracketRequest(64, 2)));

        assertEquals(ErrorCode.BRACKET_PLAN_VERSION_CONFLICT, exception.getErrorCode());
        verify(roundRepository, never()).deleteAll(any());
    }

    @Test
    void bracketOneHundredTwentyEightMustFitFinalInsideTournament() {
        UUID tournamentId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setMaxApprovedEntries(128);
        tournament.setStartDate(startDate);
        tournament.setEndDate(startDate.plusDays(20));
        tournament.setSchedulingDeadlineAt(startDate.minusDays(1).atTime(18, 0));

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(contractRepository.countByTournament_TournamentIdAndStatus(
                tournamentId, ContractStatus.APPROVED)).thenReturn(0L);

        BracketPreviewResponse tooShort = tournamentService.getBracketPreview(tournamentId);
        assertEquals(false, tooShort.isScheduleFeasible());
        assertEquals(false, tooShort.isValid());

        tournament.setEndDate(startDate.plusDays(30));
        BracketPreviewResponse enoughTime = tournamentService.getBracketPreview(tournamentId);
        assertEquals(true, enoughTime.isScheduleFeasible());
        assertEquals(true, enoughTime.isValid());
        assertEquals(LocalDate.of(2026, 8, 23), enoughTime.getEarliestPossibleEndDate());
    }

    @Test
    void confirmBracketOneHundredTwentyEightRejectsShortTournament() {
        UUID tournamentId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setName("Short 128 Cup");
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setPhase(TournamentPhase.DRAFT);
        tournament.setStartDate(startDate);
        tournament.setEndDate(startDate.plusDays(20));
        tournament.setSchedulingDeadlineAt(startDate.minusDays(1).atTime(18, 0));
        tournament.setMaxApprovedEntries(128);
        tournament.setMaxApprovedHorses(128);
        tournament.setMaxApprovedJockeys(999999);
        tournament.setBracketPlanStatus(BracketPlanStatus.NOT_GENERATED);
        tournament.setBracketPlanVersion(1);

        when(tournamentRepository.findForUpdateByTournamentId(tournamentId))
                .thenReturn(Optional.of(tournament));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(contractRepository.countByTournament_TournamentIdAndStatus(
                tournamentId, ContractStatus.APPROVED)).thenReturn(0L);

        AppException exception = assertThrows(AppException.class,
                () -> tournamentService.confirmBracket(
                        tournamentId, new ConfirmBracketRequest(128, 1)));

        assertEquals(ErrorCode.TOURNAMENT_DATE_RANGE_TOO_SHORT_FOR_BRACKET,
                exception.getErrorCode());
        verify(roundRepository, never()).save(any(com.swp391.horseracing.entity.Round.class));
        verify(raceRepository, never()).save(any(Race.class));
    }

    @Test
    void scheduleProposalReturnsTimesForEveryRoundAndRace() {
        UUID tournamentId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setMaxApprovedEntries(32);
        tournament.setStartDate(startDate);
        tournament.setEndDate(startDate.plusDays(20));
        tournament.setSchedulingDeadlineAt(startDate.minusDays(1).atTime(18, 0));
        tournament.setBracketPlanStatus(BracketPlanStatus.CONFIRMED);
        tournament.setBracketPlanVersion(2);
        tournament.setPlannedRoundCount(2);
        tournament.setPlannedRaceCount(3);

        com.swp391.horseracing.entity.Round firstRound = bracketRound(
                tournament, 1, false, 2, 4);
        com.swp391.horseracing.entity.Round finalRound = bracketRound(
                tournament, 2, true, 1, 0);
        Race firstRace = bracketRace(firstRound, 1);
        Race secondRace = bracketRace(firstRound, 2);
        Race finalRace = bracketRace(finalRound, 1);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(roundRepository.findByTournament_TournamentIdOrderBySequenceOrderAsc(tournamentId))
                .thenReturn(List.of(firstRound, finalRound));
        when(raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(firstRound.getRoundId()))
                .thenReturn(List.of(firstRace, secondRace));
        when(raceRepository.findByRound_RoundIdOrderBySequenceOrderAsc(finalRound.getRoundId()))
                .thenReturn(List.of(finalRace));

        TournamentScheduleProposalResponse response = tournamentService.getScheduleProposal(tournamentId);

        assertEquals(true, response.isFitsTournament());
        assertEquals(2, response.getRounds().size());
        assertEquals(2, response.getRounds().get(0).getRaces().size());
        assertEquals(1, response.getRounds().get(1).getRaces().size());
        assertEquals(
                response.getRounds().get(0).getSuggestedEndDate()
                        .toLocalDate().plusDays(7),
                response.getRounds().get(1).getSuggestedStartDate()
                        .toLocalDate()
        );
        assertEquals(
                tournament.getRaceDayStartTime(),
                response.getRounds().get(1).getSuggestedStartDate()
                        .toLocalTime()
        );
        assertEquals(response.getProposedFinalEndAt(),
                response.getRounds().get(1).getRaces().get(0).getSuggestedEndTime());
    }

    private com.swp391.horseracing.entity.Round bracketRound(
            Tournament tournament, int sequence, boolean isFinal, int raceCount, int qualifiers) {
        com.swp391.horseracing.entity.Round round = new com.swp391.horseracing.entity.Round();
        round.setRoundId(UUID.randomUUID());
        round.setRoundName(isFinal ? "Final" : "Round " + sequence);
        round.setTournament(tournament);
        round.setSequenceOrder(sequence);
        round.setFinal(isFinal);
        round.setMinEntries(8);
        round.setMaxEntries(16);
        round.setPlannedRaceCount(raceCount);
        round.setQualifiersPerRace(qualifiers);
        round.setBracketPlanVersion(2);
        return round;
    }

    private Race bracketRace(com.swp391.horseracing.entity.Round round, int sequence) {
        Race race = new Race();
        race.setRaceId(UUID.randomUUID());
        race.setName(round.getRoundName() + " Race " + sequence);
        race.setRound(round);
        race.setSequenceOrder(sequence);
        return race;
    }
}
