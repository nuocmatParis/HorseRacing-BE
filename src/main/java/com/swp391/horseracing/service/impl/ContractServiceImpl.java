package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.contract.request.InviteRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    JockeyHorseContractRepository contractRepository;
    TournamentRepository tournamentRepository;
    HorseTournamentRegistrationRepository horseTournamentRegistrationRepository;
    JockeyTournamentRegistrationRepository jockeyTournamentRegistrationRepository;
    HorseOwnerRepository ownerRepository;
    HorseRepository horseRepository;
    JockeyRepository jockeyRepository;
    UserRepository userRepository;
    ContractMapper contractMapper;
    UserCurrentService userCurrentService;

    @Override
    @Transactional
    public ContractResponse inviteJockey(InviteRequest request) {
        HorseOwner currentOwner = userCurrentService.getCurrentOwner();

        HorseTournamentRegistration horseTournamentRegistration = horseTournamentRegistrationRepository
                .findById(request.getTournamentRegistrationId()).orElseThrow(()
                        -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        JockeyTournamentRegistration jockeyTournamentRegistration = jockeyTournamentRegistrationRepository
                .findById(request.getJockeyTournamentRegistrationId()).orElseThrow(()
                        -> new AppException(ErrorCode.TOURNAMENT_REGISTRATION_NOT_FOUND));

        validateInvite(currentOwner, horseTournamentRegistration, jockeyTournamentRegistration, request);

        Tournament tournament = horseTournamentRegistration.getTournament();

        JockeyHorseContract contract = JockeyHorseContract.builder()
                        .tournament(tournament)
                        .horseTournamentRegistration(horseTournamentRegistration)
                        .jockeyTournamentRegistration(jockeyTournamentRegistration)
                        .owner(currentOwner)
                        .horse(horseTournamentRegistration.getHorse())
                        .jockey(jockeyTournamentRegistration.getJockey())
                        .hireFee(jockeyTournamentRegistration.getHireFee())
                        .systemContractFee(tournament.getSystemContractFee())
                        .advancePercent(30F)
                        .finalPercent(70F)
                        .ownerPrizeSharePercent(request.getOwnerPrizeSharePercent())
                        .jockeyPrizeSharePercent(request.getJockeyPrizeSharePercent())
                        .advancePaidAmount(BigDecimal.ZERO)
                        .escrowAmount(BigDecimal.ZERO)
                        .paymentStatus(ContractPaymentStatus.UNPAID)
                        .escrowStatus(EscrowStatus.NOT_HELD)
                        .advancePayoutStatus(AdvancePayoutStatus.NOT_PAID)
                        .finalPayoutStatus(FinalPayoutStatus.NOT_RELEASED)
                        .status(ContractStatus.PENDING_JOCKEY)
                        .requestedAt(LocalDateTime.now())
                        .contractNote(request.getContractNote())
                        .build();

        JockeyHorseContract savedContract = contractRepository.save(contract);

        return contractMapper.toContractResponse(savedContract);
    }

    @Override
    public List<ContractResponse> getMyInvitations() {
        User currentUser = userCurrentService.getCurrentUser();

        List<JockeyHorseContract> list = contractRepository.findByJockey_User_UserIdAndStatusOrderByRequestedAtDesc(
                currentUser.getUserId(), ContractStatus.PENDING_JOCKEY);

        List<ContractResponse> responseList = new ArrayList<>();

        for(JockeyHorseContract contract : list){
            responseList.add(contractMapper.toContractResponse(contract));
        }

        return responseList;
    }

    @Override
    public ContractResponse acceptContract(InviteRequest request) {
        return null;
    }

    @Override
    public ContractResponse rejectContractByJockey(InviteRequest request, String reason) {
        return null;
    }

    @Override
    public PaymentResponse payHiringFee(UUID contractId) {
        return null;
    }

    @Override
    public PaymentResponse payContractCreationFee(UUID contractId) {
        return null;
    }

    @Override
    public List<ContractResponse> getPendingContracts() {
        return List.of();
    }

    @Override
    public ContractResponse approveContract(UUID contractId) {
        return null;
    }

    @Override
    public ContractResponse rejectContractByAdmin(UUID contractId, String reason) {
        return null;
    }


    private void validateInvite(HorseOwner currentOwner, HorseTournamentRegistration horseTournamentRegistration,
                                JockeyTournamentRegistration jockeyTournamentRegistration, InviteRequest request){
        if(!horseTournamentRegistration.getOwner().getOwnerId().equals(currentOwner.getOwnerId()))
            throw new AppException(ErrorCode.HORSE_NOT_BELONG_TO_OWNER);

        if(horseTournamentRegistration.getStatus() != RegistrationStatus.APPROVED)
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);

        if(jockeyTournamentRegistration.getStatus() != RegistrationStatus.APPROVED)
            throw new AppException(ErrorCode.INVALID_REGISTRATION_STATUS);

        if(!jockeyTournamentRegistration.getTournament().getTournamentId().equals(horseTournamentRegistration.getTournament().getTournamentId()))
            throw new AppException(ErrorCode.TOURNAMENT_NOT_MATCH);

        if(jockeyTournamentRegistration.getHireFee() == null || jockeyTournamentRegistration.getHireFee().compareTo(BigDecimal.ZERO) <= 0)
            throw new AppException(ErrorCode.INVALID_HIRE_FEE);

        validatePrizeShare(request);

        boolean contractExists = contractRepository
                .existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
                        jockeyTournamentRegistration.getJockeyTournamentRegId(),
                        horseTournamentRegistration.getHorseRegistrationId(),
                        List.of(ContractStatus.PENDING_JOCKEY,
                                ContractStatus.ACCEPTED,
                                ContractStatus.HIRING_PAID,
                                ContractStatus.PENDING_ADMIN_REVIEW,
                                ContractStatus.APPROVED)
                );

        if(contractExists)
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS);
    }

    private void validatePrizeShare(InviteRequest request){
        Float ownerShared = request.getOwnerPrizeSharePercent();
        Float jockeyShared = request.getJockeyPrizeSharePercent();

        if(ownerShared == null || jockeyShared == null)
            throw new AppException(ErrorCode.INVALID_PRIZE_SHARE);

        if(ownerShared < 0 || ownerShared > 100 || jockeyShared < 0 || jockeyShared > 100)
            throw new AppException(ErrorCode.INVALID_PRIZE_SHARE);

        float total = ownerShared + jockeyShared;

        if(Math.abs(total - 100F) > 0.0001F)
            throw new AppException(ErrorCode.INVALID_PRIZE_SHARE);
    }
}
