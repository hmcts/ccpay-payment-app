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

    Optional<List<Payment>> findAllByReference(String reference);

    List<Reference> findReferencesByPaymentProviderAndPaymentStatusNotInAndDateCreatedLessThan(
        PaymentProvider paymentProvider, List<PaymentStatus> paymentStatuses, Date targetTime);

    Optional<List<Payment>> findAllByDateCreatedBetween(Date fromDate, Date toDate);

    Optional<List<Payment>> findByDocumentControlNumber(String documentControlNumber);

    Optional<List<Payment>> findByCcdCaseNumber(String ccdCaseNumber);

    Optional<List<Payment>> findByPaymentLinkId(Integer id);

    Optional<Payment> findByInternalReference(String internalReference);

    @Modifying
    @Query(value = "UPDATE payment SET date_updated = :rollbackdate where ccd_case_number = :ccdcasenumber",nativeQuery = true)
    int updatePaymentUpdatedDateTime(@Param("rollbackdate") LocalDateTime rollbackDate,
                                   @Param("ccdcasenumber") String ccdCaseNumber);

}
