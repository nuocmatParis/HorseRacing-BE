package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.HorseOwner;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.entity.Race;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.entity.RaceResult;
import com.swp391.horseracing.entity.Round;
import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.entity.Transaction;
import com.swp391.horseracing.entity.User;
import com.swp391.horseracing.enums.PrizeStatus;
import com.swp391.horseracing.enums.WalletOwnerType;
import com.swp391.horseracing.mapper.TransactionMapper;
import com.swp391.horseracing.repository.JockeyHorseContractRepository;
import com.swp391.horseracing.repository.RaceResultRepository;
import com.swp391.horseracing.repository.WalletRepository;
import com.swp391.horseracing.repository.WalletTransactionRepository;
import com.swp391.horseracing.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock UserCurrentService userCurrentService;
    @Mock WalletTransactionRepository transactionRepository;
    @Mock WalletRepository walletRepository;
    @Mock TransactionMapper transactionMapper;
    @Mock RaceResultRepository raceResultRepository;
    @Mock JockeyHorseContractRepository contractRepository;

    @InjectMocks TransactionServiceImpl transactionService;

    private JockeyHorseContract contract;
    private RaceResult result;

    @BeforeEach
    void setUp() {
        Tournament tournament = Tournament.builder()
                .tournamentId(UUID.randomUUID())
                .name("Giải vô địch")
                .build();
        Round round = Round.builder()
                .roundId(UUID.randomUUID())
                .roundName("Chung kết")
                .tournament(tournament)
                .build();
        Race race = Race.builder()
                .raceId(UUID.randomUUID())
                .name("Cuộc đua cuối")
                .round(round)
                .build();
        HorseOwner owner = HorseOwner.builder()
                .ownerId(UUID.randomUUID())
                .user(User.builder().userId(UUID.randomUUID()).fullName("Chủ ngựa Bình").build())
                .build();
        Jockey jockey = Jockey.builder()
                .jockeyId(UUID.randomUUID())
                .user(User.builder().userId(UUID.randomUUID()).fullName("Kỵ sĩ An").build())
                .build();
        Horse horse = Horse.builder().horseId(UUID.randomUUID()).name("Hải Đăng").owner(owner).build();
        contract = JockeyHorseContract.builder()
                .contractId(UUID.randomUUID())
                .tournament(tournament)
                .horse(horse)
                .jockey(jockey)
                .owner(owner)
                .build();
        RaceEntry entry = RaceEntry.builder()
                .entryId(UUID.randomUUID())
                .race(race)
                .contract(contract)
                .build();
        result = RaceResult.builder()
                .resultId(UUID.randomUUID())
                .race(race)
                .entry(entry)
                .rank(1)
                .prizeStatus(PrizeStatus.Paid)
                .build();
        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    return TransactionResponse.builder()
                            .transactionId(transaction.getTransactionId())
                            .raceResultId(transaction.getRaceResultId())
                            .contractId(transaction.getContractId())
                            .build();
                });
    }

    @Test
    void prizeTransactionContainsRaceTournamentHorseOwnerAndJockeyContext() {
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .raceResultId(result.getResultId())
                .contractId(contract.getContractId())
                .build();
        when(transactionRepository.findAllByWallet_OwnerTypeAndWallet_UserIsNullOrderByCreatedAtDesc(
                WalletOwnerType.SYSTEM)).thenReturn(List.of(transaction));
        when(raceResultRepository.findAllWithTransactionContextByResultIdIn(any()))
                .thenReturn(List.of(result));
        when(contractRepository.findAllWithTransactionContextByContractIdIn(any()))
                .thenReturn(List.of(contract));

        TransactionResponse response = transactionService.getSystemTransactions().get(0);

        assertEquals("Giải vô địch", response.getTournamentName());
        assertEquals("Chung kết", response.getRoundName());
        assertEquals("Cuộc đua cuối", response.getRaceName());
        assertEquals("Hải Đăng", response.getHorseName());
        assertEquals("Kỵ sĩ An", response.getJockeyName());
        assertEquals("Chủ ngựa Bình", response.getOwnerName());
        assertEquals(1, response.getFinishPosition());
        assertEquals(PrizeStatus.Paid.name(), response.getPrizeStatus());
    }

    @Test
    void finalJockeyIncomeContainsContractContextAndKeepsRaceNull() {
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID())
                .contractId(contract.getContractId())
                .build();
        when(transactionRepository.findAllByWallet_OwnerTypeAndWallet_UserIsNullOrderByCreatedAtDesc(
                WalletOwnerType.SYSTEM)).thenReturn(List.of(transaction));
        when(contractRepository.findAllWithTransactionContextByContractIdIn(any()))
                .thenReturn(List.of(contract));

        TransactionResponse response = transactionService.getSystemTransactions().get(0);

        assertEquals("Giải vô địch", response.getTournamentName());
        assertEquals("Hải Đăng", response.getHorseName());
        assertEquals("Kỵ sĩ An", response.getJockeyName());
        assertNull(response.getRaceId());
        assertNull(response.getRaceName());
    }

    @Test
    void depositWithoutBusinessReferenceStillReturnsNormally() {
        Transaction transaction = Transaction.builder().transactionId(UUID.randomUUID()).build();
        when(transactionRepository.findAllByWallet_OwnerTypeAndWallet_UserIsNullOrderByCreatedAtDesc(
                WalletOwnerType.SYSTEM)).thenReturn(List.of(transaction));

        TransactionResponse response = transactionService.getSystemTransactions().get(0);

        assertEquals(transaction.getTransactionId(), response.getTransactionId());
        assertNull(response.getTournamentId());
        assertNull(response.getHorseId());
        assertNull(response.getRaceId());
    }
}
