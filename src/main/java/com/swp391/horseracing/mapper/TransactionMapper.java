package com.swp391.horseracing.mapper;


import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "walletId", source = "wallet.walletId")
    @Mapping(target = "walletPurpose", source = "wallet.walletPurpose")
    @Mapping(target = "performedByUserId", source = "performedBy.userId")
    @Mapping(target = "performedByName", source = "performedBy.fullName")
    @Mapping(target = "tournamentId", ignore = true)
    @Mapping(target = "tournamentName", ignore = true)
    @Mapping(target = "roundId", ignore = true)
    @Mapping(target = "roundName", ignore = true)
    @Mapping(target = "raceId", ignore = true)
    @Mapping(target = "raceName", ignore = true)
    @Mapping(target = "horseId", ignore = true)
    @Mapping(target = "horseName", ignore = true)
    @Mapping(target = "jockeyId", ignore = true)
    @Mapping(target = "jockeyName", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "ownerName", ignore = true)
    @Mapping(target = "finishPosition", ignore = true)
    @Mapping(target = "prizeStatus", ignore = true)
    TransactionResponse toTransactionResponse(Transaction transaction);
}
