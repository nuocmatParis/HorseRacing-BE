package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.registration.request.RegisterJockeyRequest;
import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.dto.registration.response.JockeyTournamentRegistrationResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.HorseTournamentRegistrationMapper;
import com.swp391.horseracing.mapper.JockeyTournamentRegistrationMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.PaymentService;
import com.swp391.horseracing.service.TournamentRegistrationService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TournamentRegistrationServiceImpl implements TournamentRegistrationService {

    HorseTournamentRegistrationRepository horseRegistrationRepository;
    JockeyTournamentRegistrationRepository jockeyRegistrationRepository;
    TournamentRepository tournamentRepository;
    HorseRepository horseRepository;
    HorseOwnerRepository horseOwnerRepository;
    JockeyRepository jockeyRepository;
    UserCurrentService userCurrentService;
    HorseTournamentRegistrationMapper horseRegistrationMapper;
    JockeyTournamentRegistrationMapper jockeyRegistrationMapper;
    InvoiceService invoiceService;
    PaymentService paymentService;
    InvoiceRepository invoiceRepository;
    JockeyHorseContractRepository contractRepository;

    @Override
    @Transactional
    public HorseTournamentRegistrationResponse registerHorse(UUID tournamentId, UUID horseId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_OPEN);
        }

        if (tournament.getPhase() != TournamentPhase.REGISTRATION_OPEN) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_OPEN);
        }

        User currentUser = userCurrentService.getCurrentUser();
        HorseOwner owner = horseOwnerRepository.findByUser_UserId(currentUser.getUserId()).orElseThrow(()
                -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));

        Horse horse = horseRepository.findById(horseId)
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_NOT_FOUND));

        if (!horse.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new AppException(ErrorCode.HORSE_NOT_BELONG_TO_OWNER);
        }

        if (horseRegistrationRepository.existsByTournament_TournamentIdAndHorse_HorseId(tournamentId, horseId)) {
            throw new AppException(ErrorCode.HORSE_ALREADY_REGISTERED_TOURNAMENT);
        }

        List<RegistrationStatus> activeStatuses = List.of(
                RegistrationStatus.PENDING_PAYMENT,
                RegistrationStatus.PENDING_REVIEW,
                RegistrationStatus.APPROVED
        );

        if (horseRegistrationRepository.existsHorseWithConflictingTournament(
                horseId, tournament.getStartDate(), tournament.getEndDate(), activeStatuses)) {
            throw new AppException(ErrorCode.HORSE_TOURNAMENT_TIME_CONFLICT);
        }

        validateHorseEligibility(horse, tournament);

        HorseTournamentRegistration registration = HorseTournamentRegistration.builder()
                .tournament(tournament)
                .horse(horse)
                .owner(owner)
                .status(RegistrationStatus.PENDING_PAYMENT)
                .ratingAtRegistration(horse.getCurrentRating())
                .raceClassAtRegistration(horse.getRaceClass())
                .build();
        HorseTournamentRegistration savedRegistration = horseRegistrationRepository.save(registration);

        invoiceService.createOwnerRegistrationInvoice(currentUser.getUserId(), savedRegistration, tournament.getRegistrationFee());

        return horseRegistrationMapper.toHorseTournamentRegistrationResponse(savedRegistration);
    }

    @Override
    @Transactional
    public JockeyTournamentRegistrationResponse registerJockey(UUID tournamentId, RegisterJockeyRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_OPEN);
        }

        if (tournament.getPhase() != TournamentPhase.REGISTRATION_OPEN) {
            throw new AppException(ErrorCode.TOURNAMENT_NOT_OPEN);
        }

        Jockey jockey = getCurrentJockey();

        if (jockeyRegistrationRepository.existsByTournament_TournamentIdAndJockey_JockeyId(tournamentId, jockey.getJockeyId())) {
            throw new AppException(ErrorCode.JOCKEY_ALREADY_REGISTERED_TOURNAMENT);
        }

        List<RegistrationStatus> activeStatuses = List.of(
                RegistrationStatus.PENDING_PAYMENT,
                RegistrationStatus.PENDING_REVIEW,
                RegistrationStatus.APPROVED
        );

        if (jockeyRegistrationRepository.existsJockeyWithConflictingTournament(
                jockey.getJockeyId(), tournament.getStartDate(), tournament.getEndDate(), activeStatuses)) {
            throw new AppException(ErrorCode.JOCKEY_TOURNAMENT_TIME_CONFLICT);
        }

        validateJockeyEligibility(jockey, tournament);

        JockeyTournamentRegistration registration = JockeyTournamentRegistration.builder()
                .tournament(tournament)
                .jockey(jockey)
                .hireFee(request.getHireFee())
                .status(RegistrationStatus.PENDING_REVIEW)
                .build();
        registration = jockeyRegistrationRepository.save(registration);

        return jockeyRegistrationMapper.toJockeyTournamentRegistrationResponse(registration);
    }

    @Override
    public List<JockeyTournamentRegistrationResponse> getMyJockeyRegistrations() {
        Jockey jockey = getCurrentJockey();
        return jockeyRegistrationRepository.findByJockey_JockeyId(jockey.getJockeyId())
                .stream().map(jockeyRegistrationMapper::toJockeyTournamentRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<HorseTournamentRegistrationResponse> getMyHorseRegistrations() {
        HorseOwner owner = getCurrentOwner();
        return horseRegistrationRepository.findByOwner_OwnerId(owner.getOwnerId())
                .stream().map(horseRegistrationMapper::toHorseTournamentRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HorseTournamentRegistrationResponse approveHorseRegistration(UUID registrationId) {
        HorseTournamentRegistration registration = horseRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        validatePendingStatus(registration.getStatus());

        Tournament tournament = registration.getTournament();
        long approvedCount = horseRegistrationRepository.countByTournament_TournamentIdAndStatus(
                tournament.getTournamentId(), RegistrationStatus.APPROVED);
        if (tournament.getMaxApprovedHorses() != null && approvedCount >= tournament.getMaxApprovedHorses()) {
            throw new AppException(ErrorCode.HORSE_REGISTRATION_LIMIT_EXCEEDED);
        }

        User currentUser = userCurrentService.getCurrentUser();
        registration.setStatus(RegistrationStatus.APPROVED);
        registration.setReviewedBy(currentUser);
        registration.setReviewedAt(LocalDateTime.now());

        return horseRegistrationMapper.toHorseTournamentRegistrationResponse(horseRegistrationRepository.save(registration));
    }

    @Override
    @Transactional
    public HorseTournamentRegistrationResponse rejectHorseRegistration(UUID registrationId, String reason) {
        HorseTournamentRegistration registration = horseRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        validatePendingStatus(registration.getStatus());

        Invoice invoice = invoiceRepository.findByHorseTournamentRegistration_HorseRegistrationId(registrationId).orElseThrow(()
                -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            paymentService.refundInvoice(invoice.getInvoiceId());
        }

        User currentUser = userCurrentService.getCurrentUser();
        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setReviewedBy(currentUser);
        registration.setReviewedAt(LocalDateTime.now());
        registration.setRejectedReason(reason);

        return horseRegistrationMapper.toHorseTournamentRegistrationResponse(horseRegistrationRepository.save(registration));
    }

    @Override
    @Transactional
    public JockeyTournamentRegistrationResponse approveJockeyRegistration(UUID registrationId) {
        JockeyTournamentRegistration registration = jockeyRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_TOURNAMENT_REGISTRATION_NOT_FOUND));

        validatePendingStatus(registration.getStatus());

        Tournament tournament = registration.getTournament();
        long approvedCount = jockeyRegistrationRepository.countByTournament_TournamentIdAndStatus(
                tournament.getTournamentId(), RegistrationStatus.APPROVED);
        if (tournament.getMaxApprovedJockeys() != null && approvedCount >= tournament.getMaxApprovedJockeys()) {
            throw new AppException(ErrorCode.JOCKEY_REGISTRATION_LIMIT_EXCEEDED);
        }

        User currentUser = userCurrentService.getCurrentUser();
        registration.setStatus(RegistrationStatus.APPROVED);
        registration.setReviewedBy(currentUser);
        registration.setReviewedAt(LocalDateTime.now());

        return jockeyRegistrationMapper.toJockeyTournamentRegistrationResponse(jockeyRegistrationRepository.save(registration));
    }

    @Override
    @Transactional
    public JockeyTournamentRegistrationResponse rejectJockeyRegistration(UUID registrationId, String reason) {
        JockeyTournamentRegistration registration = jockeyRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_TOURNAMENT_REGISTRATION_NOT_FOUND));

        validatePendingStatus(registration.getStatus());

        User currentUser = userCurrentService.getCurrentUser();
        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setReviewedBy(currentUser);
        registration.setReviewedAt(LocalDateTime.now());
        registration.setRejectedReason(reason);

        return jockeyRegistrationMapper.toJockeyTournamentRegistrationResponse(jockeyRegistrationRepository.save(registration));
    }

    private void validateJockeyEligibility(Jockey jockey, Tournament tournament) {
        if (jockey.getStatus() != JockeyStatus.AVAILABLE) {
            throw new AppException(ErrorCode.JOCKEY_NOT_AVAILABLE);
        }

        List<TournamentEligibility> eligibilityRules = tournament.getEligibilityRules();
        if (eligibilityRules != null) {
            for (TournamentEligibility rule : eligibilityRules) {
                if (rule.getTargetType() == EligibilityTargetType.JOCKEY && rule.isActive()) {
                    applyJockeyEligibilityRule(jockey, rule);
                }
            }
        }
    }

    private void applyJockeyEligibilityRule(Jockey jockey, TournamentEligibility rule) {
        EligibilityCondition condition = rule.getConditionName();
        EligibilityOperator operator = rule.getConditionOperator();
        String value = rule.getConditionValue();

        boolean passed = switch (condition) {
            case AGE -> {
                int age = java.time.Period.between(jockey.getUser().getDob(), java.time.LocalDate.now()).getYears();
                yield compareInt(age, operator, Integer.parseInt(value));
            }
            case WEIGHT -> compareFloat(jockey.getWeight(), operator, Float.parseFloat(value));
            case EXPERIENCE_YEARS -> compareInt(jockey.getExperienceYears(), operator, Integer.parseInt(value));
            case JOCKEY_TIER -> compareJockeyTier(jockey.getJockeyTier(), operator, value);
            default -> true;
        };

        if (!passed) {
            throw new AppException(ErrorCode.JOCKEY_NOT_ELIGIBLE);
        }
    }

    private boolean compareJockeyTier(JockeyTier actual, EligibilityOperator operator, String expectedValue) {
        try {
            JockeyTier expected = JockeyTier.valueOf(expectedValue.toUpperCase());
            int actualOrdinal = actual.ordinal();
            int expectedOrdinal = expected.ordinal();
            return switch (operator) {
                case EQUAL -> actualOrdinal == expectedOrdinal;
                case NOT_EQUAL -> actualOrdinal != expectedOrdinal;
                case GREATER_THAN -> actualOrdinal > expectedOrdinal;
                case GREATER_THAN_OR_EQUAL -> actualOrdinal >= expectedOrdinal;
                case LESS_THAN -> actualOrdinal < expectedOrdinal;
                case LESS_THAN_OR_EQUAL -> actualOrdinal <= expectedOrdinal;
            };
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void validateHorseEligibility(Horse horse, Tournament tournament) {
        if (horse.getHealthStatus() != HealthStatus.HEALTHY) {
            throw new AppException(ErrorCode.HORSE_HEALTH_NOT_VALID);
        }

        int age = horse.getAge();
        if (age < tournament.getMinHorseAge() || age > tournament.getMaxHorseAge()) {
            throw new AppException(ErrorCode.HORSE_NOT_ELIGIBLE);
        }

        if (tournament.getAllowedBreed() != horse.getBreed()) {
            throw new AppException(ErrorCode.HORSE_NOT_ELIGIBLE);
        }

        if (tournament.getRaceClass() != null && !tournament.getRaceClass().isEligible(horse.getCurrentRating())) {
            throw new AppException(ErrorCode.HORSE_NOT_ELIGIBLE);
        }

        List<TournamentEligibility> eligibilityRules = tournament.getEligibilityRules();
        if (eligibilityRules != null) {
            for (TournamentEligibility rule : eligibilityRules) {
                if (rule.getTargetType() == EligibilityTargetType.HORSE && rule.isActive()) {
                    applyEligibilityRule(horse, rule);
                }
            }
        }
    }

    private void applyEligibilityRule(Horse horse, TournamentEligibility rule) {
        EligibilityCondition condition = rule.getConditionName();
        EligibilityOperator operator = rule.getConditionOperator();
        String value = rule.getConditionValue();

        boolean passed = switch (condition) {
            case AGE -> compareInt(horse.getAge(), operator, Integer.parseInt(value));
            case WEIGHT -> compareFloat(horse.getWeight(), operator, Float.parseFloat(value));
            case WIN_RATE -> compareDouble(horse.getWinRate(), operator, Double.parseDouble(value));
            case BREED -> compareBreed(horse.getBreed(), operator, value);
            default -> true;
        };

        if (!passed) {
            throw new AppException(ErrorCode.HORSE_NOT_ELIGIBLE);
        }
    }

    private boolean compareBreed(HorseBreed actual, EligibilityOperator operator, String expectedValue) {
        try {
            HorseBreed expected = HorseBreed.valueOf(expectedValue.toUpperCase());
            return switch (operator) {
                case EQUAL -> actual == expected;
                case NOT_EQUAL -> actual != expected;
                default -> false;
            };
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean compareInt(int actual, EligibilityOperator operator, int expected) {
        return switch (operator) {
            case GREATER_THAN_OR_EQUAL -> actual >= expected;
            case LESS_THAN_OR_EQUAL -> actual <= expected;
            case GREATER_THAN -> actual > expected;
            case LESS_THAN -> actual < expected;
            case EQUAL -> actual == expected;
            case NOT_EQUAL -> actual != expected;
        };
    }

    private boolean compareFloat(float actual, EligibilityOperator operator, float expected) {
        return switch (operator) {
            case GREATER_THAN_OR_EQUAL -> actual >= expected;
            case LESS_THAN_OR_EQUAL -> actual <= expected;
            case GREATER_THAN -> actual > expected;
            case LESS_THAN -> actual < expected;
            case EQUAL -> actual == expected;
            case NOT_EQUAL -> actual != expected;
        };
    }

    private boolean compareDouble(double actual, EligibilityOperator operator, double expected) {
        return switch (operator) {
            case GREATER_THAN_OR_EQUAL -> actual >= expected;
            case LESS_THAN_OR_EQUAL -> actual <= expected;
            case GREATER_THAN -> actual > expected;
            case LESS_THAN -> actual < expected;
            case EQUAL -> actual == expected;
            case NOT_EQUAL -> actual != expected;
        };
    }

    private void validatePendingStatus(RegistrationStatus status) {
        if (status != RegistrationStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_PENDING);
        }
    }

    private HorseOwner getCurrentOwner() {
        User user = userCurrentService.getCurrentUser();
        return horseOwnerRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));
    }

    private Jockey getCurrentJockey() {
        User user = userCurrentService.getCurrentUser();
        return jockeyRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional
    public List<HorseTournamentRegistrationResponse> getAllHorseRegistrations(){
        return horseRegistrationRepository.findAll()
                .stream().map(horseRegistrationMapper :: toHorseTournamentRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<JockeyTournamentRegistrationResponse> getAllJockeyRegistrations(){
        return jockeyRegistrationRepository.findAll()
                .stream().map(jockeyRegistrationMapper :: toJockeyTournamentRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<JockeyTournamentRegistrationResponse> getApprovedJockeysByTournament(UUID tournamentId) {
        return jockeyRegistrationRepository
                .findByTournament_TournamentIdAndStatus(tournamentId, RegistrationStatus.APPROVED)
                .stream()
                .map(jockeyRegistrationMapper::toJockeyTournamentRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<HorseTournamentRegistrationResponse> getApprovedHorsesByTournament(UUID tournamentId) {
        return horseRegistrationRepository
                .findByTournament_TournamentIdAndStatus(tournamentId, RegistrationStatus.APPROVED)
                .stream()
                .map(horseRegistrationMapper::toHorseTournamentRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HorseTournamentRegistrationResponse withdrawHorseRegistration(UUID registrationId, String reason) {
        HorseTournamentRegistration registration = horseRegistrationRepository.findForUpdateById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));
        User currentUser = userCurrentService.getCurrentUser();

        if (registration.getOwner() == null
                || !registration.getOwner().getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        if (registration.getStatus() == RegistrationStatus.WITHDRAWN) {
            throw new AppException(ErrorCode.REGISTRATION_ALREADY_WITHDRAWN);
        }
        if (!isWithdrawableStatus(registration.getStatus())
                || !isWithdrawablePhase(registration.getTournament().getPhase())) {
            throw new AppException(ErrorCode.REGISTRATION_WITHDRAW_NOT_ALLOWED);
        }

        List<ContractStatus> blockingStatuses = Arrays.asList(
                ContractStatus.ACCEPTED,
                ContractStatus.HIRING_PAID,
                ContractStatus.PENDING_ADMIN_REVIEW,
                ContractStatus.APPROVED
        );
        List<JockeyHorseContract> activeContracts = contractRepository
                .findByHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
                        registrationId, blockingStatuses);
        if (!activeContracts.isEmpty()) {
            throw new AppException(ErrorCode.REGISTRATION_HAS_ACTIVE_CONTRACT);
        }

        List<JockeyHorseContract> invitations = contractRepository
                .findByHorseTournamentRegistration_HorseRegistrationIdAndStatus(
                        registrationId, ContractStatus.PENDING_JOCKEY);
        LocalDateTime now = LocalDateTime.now();
        for (JockeyHorseContract invitation : invitations) {
            invitation.setStatus(ContractStatus.CANCELLED);
            invitation.setCancelledAt(now);
            invitation.setCancelReason("Owner withdrew the horse registration: " + reason.trim());
            invitation.setAdvancePayoutStatus(AdvancePayoutStatus.CANCELLED);
            invitation.setFinalPayoutStatus(FinalPayoutStatus.CANCELLED);
            contractRepository.save(invitation);
        }

        java.util.Optional<Invoice> invoice = invoiceRepository
                .findByHorseTournamentRegistration_HorseRegistrationId(registrationId);
        if (invoice.isPresent() && invoice.get().getStatus() == InvoiceStatus.UNPAID) {
            invoiceService.cancelInvoice(invoice.get().getInvoiceId());
        }

        registration.setStatus(RegistrationStatus.WITHDRAWN);
        registration.setWithdrawnAt(now);
        registration.setWithdrawReason(reason.trim());
        return horseRegistrationMapper.toHorseTournamentRegistrationResponse(
                horseRegistrationRepository.save(registration));
    }

    private boolean isWithdrawableStatus(RegistrationStatus status) {
        return status == RegistrationStatus.PENDING_PAYMENT
                || status == RegistrationStatus.PENDING_REVIEW
                || status == RegistrationStatus.APPROVED;
    }

    private boolean isWithdrawablePhase(TournamentPhase phase) {
        return phase == TournamentPhase.REGISTRATION_OPEN
                || phase == TournamentPhase.REGISTRATION_REVIEW
                || phase == TournamentPhase.JOCKEY_MATCHING
                || phase == TournamentPhase.SCHEDULING;
    }
}
