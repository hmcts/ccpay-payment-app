package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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
}
