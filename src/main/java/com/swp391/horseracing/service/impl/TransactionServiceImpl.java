package com.swp391.horseracing.service.impl;

import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.dto.wallet.response.WalletResponse;
import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.entity.Wallet;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.enums.WalletPurpose;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.mapper.TransactionMapper;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.service.TransactionService;
import com.swp391.horseracing.service.UserCurrentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.swp391.horseracing.enums.WalletPurpose.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    UserCurrentService userCurrentService;
    WalletTransactionRepository transactionRepository;
    WalletRepository walletRepository;
    TransactionMapper transactionMapper;
    RaceResultRepository raceResultRepository;
    JockeyHorseContractRepository contractRepository;

    static Set<WalletPurpose> walletPurposes = Set.of(
            SYSTEM_REVENUE,
            SYSTEM_ESCROW,
            SYSTEM_PRIZE_POOL
    );


    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getMyTransactions() {
        User currentUser = userCurrentService.getCurrentUser();

        Wallet wallet = walletRepository.findByUser_UserIdAndWalletPurpose(
                currentUser.getUserId(), WalletPurpose.USER_MAIN).
                orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        List<Transaction> transactions = transactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(
                wallet.getWalletId());
        return toEnrichedResponses(transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getSystemTransactions() {
        List<Transaction> list = transactionRepository.findAllByWallet_OwnerTypeAndWallet_UserIsNullOrderByCreatedAtDesc(WalletOwnerType.SYSTEM);

        return toEnrichedResponses(list);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getSystemTransactions(WalletPurpose walletPurpose) {
        if(!walletPurposes.contains(walletPurpose)){
            throw new AppException(ErrorCode.INVALID_SYSTEM_WALLET_PURPOSE);
        }

        Wallet wallet = walletRepository.findFirstByOwnerTypeAndUserIsNullAndWalletPurpose(WalletOwnerType.SYSTEM,
                walletPurpose).orElseThrow(() -> new AppException(ErrorCode.SYSTEM_WALLET_NOT_FOUND));

        List<Transaction> list = transactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId());

        return toEnrichedResponses(list);
    }

    private List<TransactionResponse> toEnrichedResponses(List<Transaction> transactions) {
        Set<UUID> resultIds = new HashSet<>();
        Set<UUID> contractIds = new HashSet<>();
        for (Transaction transaction : transactions) {
            if (transaction.getRaceResultId() != null) {
                resultIds.add(transaction.getRaceResultId());
            }
            if (transaction.getContractId() != null) {
                contractIds.add(transaction.getContractId());
            }
        }

        Map<UUID, RaceResult> resultById = new HashMap<>();
        if (!resultIds.isEmpty()) {
            List<RaceResult> results = raceResultRepository
                    .findAllWithTransactionContextByResultIdIn(resultIds);
            for (RaceResult result : results) {
                resultById.put(result.getResultId(), result);
            }
        }

        Map<UUID, JockeyHorseContract> contractById = new HashMap<>();
        if (!contractIds.isEmpty()) {
            List<JockeyHorseContract> contracts = contractRepository
                    .findAllWithTransactionContextByContractIdIn(contractIds);
            for (JockeyHorseContract contract : contracts) {
                contractById.put(contract.getContractId(), contract);
            }
        }

        List<TransactionResponse> responses = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionResponse response = transactionMapper.toTransactionResponse(transaction);
            RaceResult result = resultById.get(transaction.getRaceResultId());
            if (result != null) {
                applyRaceResultContext(response, result);
            } else {
                JockeyHorseContract contract = contractById.get(transaction.getContractId());
                if (contract != null) {
                    applyContractContext(response, contract);
                }
            }
            responses.add(response);
        }
        return responses;
    }

    private void applyRaceResultContext(TransactionResponse response, RaceResult result) {
        Race race = result.getRace();
        response.setRaceId(race.getRaceId());
        response.setRaceName(race.getName());
        response.setRoundId(race.getRound().getRoundId());
        response.setRoundName(race.getRound().getRoundName());
        response.setTournamentId(race.getRound().getTournament().getTournamentId());
        response.setTournamentName(race.getRound().getTournament().getName());
        response.setFinishPosition(result.getRank());
        if (result.getPrizeStatus() != null) {
            response.setPrizeStatus(result.getPrizeStatus().name());
        }
        applyContractContext(response, result.getEntry().getContract());
    }

    private void applyContractContext(
            TransactionResponse response,
            JockeyHorseContract contract) {
        response.setTournamentId(contract.getTournament().getTournamentId());
        response.setTournamentName(contract.getTournament().getName());
        response.setHorseId(contract.getHorse().getHorseId());
        response.setHorseName(contract.getHorse().getName());
        response.setJockeyId(contract.getJockey().getJockeyId());
        response.setJockeyName(contract.getJockey().getUser().getFullName());
        response.setOwnerId(contract.getOwner().getOwnerId());
        response.setOwnerName(contract.getOwner().getUser().getFullName());
    }
}
