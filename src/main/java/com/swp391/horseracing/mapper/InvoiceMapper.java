package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.invoice.response.InvoiceResponse;
import com.swp391.horseracing.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "tournamentRegId",
            source = "horseTournamentRegistration.registrationId")
    InvoiceResponse toInvoiceResponse(Invoice invoice);

}
