package com.swp391.horseracing.mapper;


import com.swp391.horseracing.dto.transaction.response.TransactionResponse;
import com.swp391.horseracing.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "walletId", source = "wallet.walletId")
    @Mapping(target = "walletPurpose", source = "wallet.walletPurpose")
    TransactionResponse toTransactionResponse(Transaction transaction);
}
