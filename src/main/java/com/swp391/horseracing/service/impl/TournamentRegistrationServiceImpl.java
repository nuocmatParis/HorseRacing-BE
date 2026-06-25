package com.swp391.horseracing.service.impl;

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

    @Override
    @Transactional
    public HorseTournamentRegistrationResponse registerHorse(UUID tournamentId, UUID horseId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.OPEN) {
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
                .build();
        HorseTournamentRegistration savedRegistration = horseRegistrationRepository.save(registration);

        invoiceService.createOwnerRegistrationInvoice(currentUser.getUserId(), savedRegistration, tournament.getRegistrationFee());

        return horseRegistrationMapper.toHorseTournamentRegistrationResponse(savedRegistration);
    }

    @Override
    @Transactional
    public JockeyTournamentRegistrationResponse registerJockey(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.OPEN) {
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
                .status(RegistrationStatus.PENDING_PAYMENT)
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

        Invoice invoice = invoiceRepository.findByHorseTournamentRegistration_RegistrationId(registrationId).orElseThrow(()
                -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        paymentService.refundInvoice(invoice.getInvoiceId());

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
        String condition = rule.getConditionName().toLowerCase();
        String operator = rule.getConditionOperator();
        String value = rule.getConditionValue();

        boolean passed = switch (condition) {
            case "height" -> compareFloat(jockey.getHeight(), operator, Float.parseFloat(value));
            case "weight" -> compareFloat(jockey.getWeight(), operator, Float.parseFloat(value));
            case "experienceyears" -> compareInt(jockey.getExperienceYears(), operator, Integer.parseInt(value));
            default -> true;
        };

        if (!passed) {
            throw new AppException(ErrorCode.JOCKEY_NOT_ELIGIBLE);
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

        List<String> allowedBreeds = Arrays.stream(tournament.getAllowedBreed().split("\\s*,\\s*"))
                .map(String::toUpperCase)
                .toList();
        if (allowedBreeds.stream().noneMatch(b -> b.equals(horse.getBreed().name()))) {
            throw new AppException(ErrorCode.HORSE_NOT_ELIGIBLE);
        }

        if (!tournament.getRaceClass().equalsIgnoreCase(horse.getRaceClass())) {
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
        String condition = rule.getConditionName().toLowerCase();
        String operator = rule.getConditionOperator();
        String value = rule.getConditionValue();

        boolean passed = switch (condition) {
            case "age" -> compareInt(horse.getAge(), operator, Integer.parseInt(value));
            case "weight" -> compareFloat(horse.getWeight(), operator, Float.parseFloat(value));
            case "winrate" -> compareDouble(horse.getWinRate(), operator, Double.parseDouble(value));
            case "totalraces" -> compareInt(horse.getTotalRaces(), operator, Integer.parseInt(value));
            case "totalwins" -> compareInt(horse.getTotalWins(), operator, Integer.parseInt(value));
            case "raceclass" -> value.equalsIgnoreCase(horse.getRaceClass());
            case "breed" -> value.equalsIgnoreCase(horse.getBreed().name());
            default -> true;
        };

        if (!passed) {
            throw new AppException(ErrorCode.HORSE_NOT_ELIGIBLE);
        }
    }

    private boolean compareInt(int actual, String operator, int expected) {
        return switch (operator) {
            case ">=" -> actual >= expected;
            case "<=" -> actual <= expected;
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            case "==" -> actual == expected;
            case "!=" -> actual != expected;
            default -> true;
        };
    }

    private boolean compareFloat(float actual, String operator, float expected) {
        return switch (operator) {
            case ">=" -> actual >= expected;
            case "<=" -> actual <= expected;
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            default -> true;
        };
    }

    private boolean compareDouble(double actual, String operator, double expected) {
        return switch (operator) {
            case ">=" -> actual >= expected;
            case "<=" -> actual <= expected;
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            default -> true;
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
}
