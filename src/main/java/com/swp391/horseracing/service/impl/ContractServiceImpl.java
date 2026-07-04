package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.contract.request.CreateContractRequest;
import com.swp391.horseracing.dto.contract.request.UpdateContractRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.ContractStatus;
import com.swp391.horseracing.enums.RegistrationStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.ContractMapper;
import com.swp391.horseracing.repository.*;
import com.swp391.horseracing.service.ContractService;
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
public class ContractServiceImpl implements ContractService {

    JockeyHorseContractRepository contractRepository;
    TournamentRepository tournamentRepository;
    HorseTournamentRegistrationRepository tournamentRegistrationRepository;
    JockeyTournamentRegistrationRepository jockeyTournamentRegistrationRepository;
    HorseOwnerRepository ownerRepository;
    HorseRepository horseRepository;
    JockeyRepository jockeyRepository;
    UserRepository userRepository;
    ContractMapper contractMapper;
    UserCurrentService userCurrentService;

    @Override
    @Transactional
    public ContractResponse create(CreateContractRequest request) {
        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_NOT_FOUND));

        HorseTournamentRegistration tournamentReg = tournamentRegistrationRepository
                .findById(request.getTournamentRegId())
                .orElseThrow(() -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        JockeyTournamentRegistration jockeyTournamentReg = jockeyTournamentRegistrationRepository
                .findById(request.getJockeyTournamentRegId())
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_TOURNAMENT_REGISTRATION_NOT_FOUND));

        HorseOwner owner = ownerRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new AppException(ErrorCode.OWNER_PROFILE_NOT_FOUND));

        Horse horse = horseRepository.findById(request.getHorseId())
                .orElseThrow(() -> new AppException(ErrorCode.HORSE_NOT_FOUND));

        Jockey jockey = jockeyRepository.findById(request.getJockeyId())
                .orElseThrow(() -> new AppException(ErrorCode.JOCKEY_PROFILE_NOT_FOUND));

        if (Math.abs(request.getAdvancePercent() + request.getFinalPercent() - 100.0f) >= 0.01f) {
            throw new AppException(ErrorCode.INVALID_CONTRACT_PERCENTAGES);
        }

        if (Math.abs(request.getOwnerPrizeSharePercent() + request.getJockeyPrizeSharePercent() - 100.0f) >= 0.01f) {
            throw new AppException(ErrorCode.INVALID_PRIZE_SHARE_PERCENTAGES);
        }

        if (contractRepository.existsByTournament_TournamentIdAndHorse_HorseId(
                request.getTournamentId(), request.getHorseId())) {
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS_FOR_HORSE);
        }

        if (contractRepository.findByTournamentRegistration_HorseRegistrationId(
                request.getTournamentRegId()).isPresent()) {
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS_FOR_REGISTRATION);
        }

        User currentUser = userCurrentService.getCurrentUser();

        JockeyHorseContract contract = contractMapper.toContract(request);
        contract.setTournament(tournament);
        contract.setTournamentRegistration(tournamentReg);
        contract.setJockeyTournamentRegistration(jockeyTournamentReg);
        contract.setOwner(owner);
        contract.setHorse(horse);
        contract.setJockey(jockey);
        contract.setStatus(ContractStatus.PENDING_JOCKEY);

        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    @Override
    @Transactional
    public ContractResponse updateStatus(UUID contractId, UpdateContractRequest request) {
        JockeyHorseContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (request.getStatus() == null) {
            throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS);
        }

        ContractStatus currentStatus = contract.getStatus();
        ContractStatus newStatus = request.getStatus();

        if (currentStatus == newStatus) {
            return contractMapper.toContractResponse(contract);
        }

        switch (currentStatus) {
            case PENDING_JOCKEY -> {
                if (newStatus == ContractStatus.ACCEPTED) {
                    contract.setStatus(ContractStatus.ACCEPTED);
                    contract.setAcceptedAt(LocalDateTime.now());
                } else if (newStatus == ContractStatus.REJECTED) {
                    contract.setStatus(ContractStatus.REJECTED);
                    contract.setRejectedReason(request.getRejectedReason());
                } else if (newStatus == ContractStatus.CANCELLED) {
                    contract.setStatus(ContractStatus.CANCELLED);
                    contract.setCancelledAt(LocalDateTime.now());
                    contract.setCancelReason(request.getCancelReason());
                } else {
                    throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS_TRANSITION);
                }
            }
            case ACCEPTED -> {
                if (newStatus == ContractStatus.HIRING_PAID) {
                    contract.setStatus(ContractStatus.HIRING_PAID);
                } else if (newStatus == ContractStatus.CANCELLED) {
                    contract.setStatus(ContractStatus.CANCELLED);
                    contract.setCancelledAt(LocalDateTime.now());
                    contract.setCancelReason(request.getCancelReason());
                } else {
                    throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS_TRANSITION);
                }
            }
            case HIRING_PAID -> {
                if (newStatus == ContractStatus.PENDING_ADMIN_REVIEW) {
                    contract.setStatus(ContractStatus.PENDING_ADMIN_REVIEW);
                    contract.setSubmittedAt(LocalDateTime.now());
                } else if (newStatus == ContractStatus.CANCELLED) {
                    contract.setStatus(ContractStatus.CANCELLED);
                    contract.setCancelledAt(LocalDateTime.now());
                    contract.setCancelReason(request.getCancelReason());
                } else {
                    throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS_TRANSITION);
                }
            }
            case PENDING_ADMIN_REVIEW -> {
                if (newStatus == ContractStatus.APPROVED) {
                    User currentUser = userCurrentService.getCurrentUser();
                    contract.setStatus(ContractStatus.APPROVED);
                    contract.setReviewedBy(currentUser);
                    contract.setReviewedAt(LocalDateTime.now());
                } else if (newStatus == ContractStatus.REJECTED) {
                    User currentUser = userCurrentService.getCurrentUser();
                    contract.setStatus(ContractStatus.REJECTED);
                    contract.setReviewedBy(currentUser);
                    contract.setReviewedAt(LocalDateTime.now());
                    contract.setRejectedReason(request.getRejectedReason());
                } else {
                    throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS_TRANSITION);
                }
            }
            case APPROVED -> {
                if (newStatus == ContractStatus.TERMINATED) {
                    contract.setStatus(ContractStatus.TERMINATED);
                    contract.setTerminatedAt(LocalDateTime.now());
                } else if (newStatus == ContractStatus.CANCELLED) {
                    contract.setStatus(ContractStatus.CANCELLED);
                    contract.setCancelledAt(LocalDateTime.now());
                    contract.setCancelReason(request.getCancelReason());
                } else {
                    throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS_TRANSITION);
                }
            }
            case REJECTED, CANCELLED, TERMINATED ->
                    throw new AppException(ErrorCode.INVALID_CONTRACT_STATUS_TRANSITION);
        }

        if (request.getContractNote() != null) {
            contract.setContractNote(request.getContractNote());
        }

        return contractMapper.toContractResponse(contractRepository.save(contract));
    }

    @Override
    public ContractResponse getContractById(UUID contractId) {
        JockeyHorseContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        return contractMapper.toContractResponse(contract);
    }

    @Override
    public List<ContractResponse> getContractsByTournament(UUID tournamentId) {
        return contractRepository.findByTournament_TournamentId(tournamentId)
                .stream()
                .map(contractMapper::toContractResponse)
                .toList();
    }

    @Override
    public List<ContractResponse> getContractsByOwner(UUID ownerId) {
        return contractRepository.findByOwner_OwnerId(ownerId)
                .stream()
                .map(contractMapper::toContractResponse)
                .toList();
    }

    @Override
    public List<ContractResponse> getContractsByJockey(UUID jockeyId) {
        return contractRepository.findByJockey_JockeyId(jockeyId)
                .stream()
                .map(contractMapper::toContractResponse)
                .toList();
    }

    @Override
    public List<ContractResponse> getApprovedContractsByTournament(UUID tournamentId) {
        return contractRepository.findByTournament_TournamentIdAndStatus(tournamentId, ContractStatus.APPROVED)
                .stream()
                .map(contractMapper::toContractResponse)
                .toList();
    }
}
