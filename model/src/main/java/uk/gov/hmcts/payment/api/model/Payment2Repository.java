package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.payment.api.dto.Reference;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface Payment2Repository extends CrudRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByReferenceAndPaymentMethod(String reference, PaymentMethod paymentMethod);

    Optional<Payment> findByReferenceAndPaymentProvider(String reference, PaymentProvider paymentProvider);

    Optional<Payment> findByReference(String reference);

    List<Reference> findReferencesByPaymentProviderAndPaymentStatusNotInAndDateCreatedLessThan(
        PaymentProvider paymentProvider, List<PaymentStatus> paymentStatuses, Date targetTime);

    Optional<List<Payment>> findAllByDateCreatedBetween(Date fromDate, Date toDate);

    Optional<List<Payment>> findByDocumentControlNumber(String documentControlNumber);

    Optional<List<Payment>> findByCcdCaseNumber(String ccdCaseNumber);

    Optional<List<Payment>> findByPaymentLinkId(Integer id);

    @Modifying
    @Query("update payment p set p.date_updated = :rollbackDate where p.ccd_case_number = :ccdCaseNumber")
    int updatePaymentUpdatedDateTime(@Param("rollbackDate") Date rollbackDate,
                                   @Param("ccdCaseNumber") String ccdCaseNumber);

}
