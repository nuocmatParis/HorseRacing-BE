package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Invoice;
import com.swp391.horseracing.enums.InvoiceStatus;
import com.swp391.horseracing.enums.InvoiceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findAllByPayerUser_UserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Invoice> findByHorseTournamentRegistration_HorseRegistrationId(UUID tournamentRegId);


    Optional<Invoice> findByContractIdAndInvoiceType(UUID contractId, InvoiceType invoiceType);

    boolean existsByHorseTournamentRegistration_HorseRegistrationIdAndInvoiceType(UUID tournamentRegId, InvoiceType invoiceType);

    boolean existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndInvoiceType(
            UUID jockeyTournamentRegId, InvoiceType invoiceType);

    Optional<Invoice> findByJockeyTournamentRegistration_JockeyTournamentRegId(UUID jockeyTournamentRegId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invoice> findForUpdateByInvoiceId(UUID invoiceId);

    List<Invoice> findAllByStatusOrderByCreatedAtDesc(InvoiceStatus invoiceStatus);

    boolean existsByContractIdAndInvoiceType(UUID contractId, InvoiceType invoiceType);

}
