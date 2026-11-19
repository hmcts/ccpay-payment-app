package uk.gov.hmcts.payment.api.model;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentFeeLinkRepository extends CrudRepository<PaymentFeeLink, Integer>, JpaSpecificationExecutor<PaymentFeeLink> {

    <S extends PaymentFeeLink> S save(S entity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select serviceRequest from PaymentFeeLink serviceRequest where serviceRequest.id = :id")
    Optional<PaymentFeeLink> findByIdForUpdate(@Param("id") Integer id);

    Optional<PaymentFeeLink> findByPaymentReference(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select paymentFeeLink from PaymentFeeLink paymentFeeLink where paymentFeeLink.paymentReference = :paymentReference")
    Optional<PaymentFeeLink> findByPaymentReferenceForUpdate(@Param("paymentReference") String paymentReference);

    Optional<PaymentFeeLink> findByPaymentReferenceAndCcdCaseNumber(String id, String ccdCaseNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select pfl
        from PaymentFeeLink pfl
        where pfl.paymentReference = :paymentReference
          and pfl.ccdCaseNumber = :ccdCaseNumber
        """)
    Optional<PaymentFeeLink> findByPaymentReferenceAndCcdCaseNumberForUpdate(
        @Param("paymentReference") String paymentReference,
        @Param("ccdCaseNumber") String ccdCaseNumber);

    Optional<List<PaymentFeeLink>> findByCcdCaseNumber(String ccdCaseNumber);

    @Query(value = "SELECT pfl.* FROM payment_fee_link pfl "
        + "LEFT JOIN fee f ON f.payment_link_id = pfl.id "
        + "WHERE pfl.payment_reference = cast(:paymentReference as text) "
        + "AND f.id = cast(:feeId as bigInt)",
        nativeQuery = true)
    Optional<PaymentFeeLink> findByPaymentReferenceAndFeeId(final @Param("paymentReference") String paymentReference,
                                         final @Param("feeId") Integer feeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select pfl
        from PaymentFeeLink pfl
        where pfl.paymentReference = :paymentReference
          and exists (
              select fee.id
              from PaymentFee fee
              where fee.paymentLink = pfl
                and fee.id = :feeId
          )
        """)
    Optional<PaymentFeeLink> findByPaymentReferenceAndFeeIdForUpdate(
        @Param("paymentReference") String paymentReference,
        @Param("feeId") Integer feeId);

    @Query(value = "SELECT concat(date_part('year', now()),'-',nextval('payment_reference_seq'))", nativeQuery = true)
    String getNextPaymentReference();

    @Query(value = "SELECT STRING_AGG(fee.code, '|' order by fee.code) AS fee_codes, fee.payment_link_id, fee.ccd_case_number,  pfl.enterprise_service_name " +
        "FROM fee " +
        "JOIN payment_fee_link pfl ON pfl.id = fee.payment_link_id " +
        "WHERE fee.ccd_case_number IN (SELECT fee.ccd_case_number " +
        "FROM fee " +
        "JOIN payment_fee_link pfl ON pfl.id = fee.payment_link_id " +
        "WHERE date_trunc('day', pfl.date_created) = :date " +
        "GROUP BY fee.code, fee.ccd_case_number, pfl.enterprise_service_name " +
        "HAVING COUNT(*) > 1) " +
        "AND date_trunc('day', pfl.date_created) = :date " +
        "GROUP BY fee.ccd_case_number, fee.payment_link_id, pfl.enterprise_service_name;",nativeQuery = true)
    Optional<List<DuplicateServiceRequestDto>> getDuplicates(final @Param("date") LocalDate date);

}
