package uk.gov.hmcts.payment.api.model;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.payment.api.dto.DuplicatePaymentDto;
import uk.gov.hmcts.payment.api.dto.Reference;

import jakarta.persistence.Tuple;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface Payment2Repository extends CrudRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByReferenceAndPaymentMethod(String reference, PaymentMethod paymentMethod);

    Optional<Payment> findByReferenceAndPaymentProvider(String reference, PaymentProvider paymentProvider);

    Optional<Payment> findByReference(String reference);

    List<Reference> findReferencesByPaymentProviderAndPaymentStatusNotInAndDateCreatedLessThan(
            PaymentProvider paymentProvider, List<PaymentStatus> paymentStatuses, Date targetTime,
            Sort dateCreated);

    Optional<List<Payment>> findAllByDateCreatedBetween(Date fromDate, Date toDate);

    Optional<List<Payment>> findByDocumentControlNumber(String documentControlNumber);

    Optional<List<Payment>> findByCcdCaseNumber(String ccdCaseNumber);

    Optional<List<Payment>> findByPaymentLinkId(Integer id);

    Optional<Payment> findByInternalReference(String internalReference);

    long deleteByReference(String reference);

    List<Payment> findByReferenceIn(List<String> reference);

    List<Payment> findByDocumentControlNumberInAndPaymentMethod(List<String> dcn, PaymentMethod paymentMethod);

    @Modifying
    @Query(value = "UPDATE payment SET date_updated = :rollbackdate where ccd_case_number = :ccdcasenumber",nativeQuery = true)
    int updatePaymentUpdatedDateTime(@Param("rollbackdate") LocalDateTime rollbackDate,
                                   @Param("ccdcasenumber") String ccdCaseNumber);

    @Query(value = "SELECT max(date_updated),ccd_case_number,service_type,amount,payment_channel,payment_method,payment_link_id,COUNT(*) as count "
        + "FROM Payment "
        + "GROUP BY ccd_case_number,service_type,amount,payment_channel,payment_method,payment_link_id,payment_status "
        + "HAVING COUNT(*) > 1 AND payment_status = 'success' AND "
        + "max(date_updated) >= :startDate AND max(date_updated) < :endDate "
        + "ORDER BY service_type, payment_method, COUNT(*) DESC",
        nativeQuery = true)
    List<Tuple> findDuplicatePaymentsByDate(@Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);

    @Query("SELECT p.serviceType, p.ccdCaseNumber, p.reference, f.code, p.dateCreated, p.amount, p.paymentStatus " +
            "FROM Payment p " +
            "JOIN p.paymentFeeLink pfl " +
            "JOIN pfl.fees f " +
            "WHERE p.dateCreated BETWEEN :fromDate AND :toDate AND p.paymentChannel.name = :paymentChannel")
    List<Tuple> findAllByDateCreatedBetweenAndPaymentChannel(
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("paymentChannel") String paymentChannel);

}
