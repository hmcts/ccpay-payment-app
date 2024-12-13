package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentFeeLinkRepository extends CrudRepository<PaymentFeeLink, Integer>, JpaSpecificationExecutor<PaymentFeeLink> {

    <S extends PaymentFeeLink> S save(S entity);

    Optional<PaymentFeeLink> findByPaymentReference(String id);

    Optional<PaymentFeeLink> findByPaymentReferenceAndCcdCaseNumber(String id, String ccdCaseNumber);

    Optional<List<PaymentFeeLink>> findByCcdCaseNumber(String ccdCaseNumber);

    @Query(value = "SELECT pfl.* FROM payment_fee_link pfl "
        + "LEFT JOIN fee f ON f.payment_link_id = pfl.id "
        + "WHERE pfl.payment_reference = cast(:paymentReference as text) "
        + "AND f.id = cast(:feeId as bigInt)",
        nativeQuery = true)
    Optional<PaymentFeeLink> findByPaymentReferenceAndFeeId(final @Param("paymentReference") String paymentReference,
                                         final @Param("feeId") Integer feeId);

    @Query(value = "SELECT concat(date_part('year', now()),'-',nextval('payment_reference_seq'))", nativeQuery = true)
    String getNextPaymentReference();

    @Query(value = "SELECT STRING_AGG(fee.code, '| ') AS fee_codes, fee.payment_link_id, fee.ccd_case_number,  pfl.enterprise_service_name " +
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
