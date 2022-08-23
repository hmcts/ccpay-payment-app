package uk.gov.hmcts.payment.api.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PaymentFailureRepository extends CrudRepository<PaymentFailures, Integer> {

    Optional<PaymentFailures> findByFailureReference(String failureReference);
    Optional<List<PaymentFailures>> findByPaymentReferenceOrderByFailureEventDateTimeDesc(String paymentReference);
    Optional<List<PaymentFailures>> findByPaymentReference(String paymentReference);
    long deleteByFailureReference(String failureReference);

    @Query("select pf from PaymentFailures pf " +
        "where pf.failureEventDateTime between ?1 and ?2 or pf.representmentOutcomeDate between ?1 and ?2 order by pf.failureEventDateTime desc")
    List<PaymentFailures> findByDatesBetween(Date fromDate, Date toDate);

    @Query("select pf from PaymentFailures pf where  (paymentReference IS NULL or paymentReference ='') and dcn IS NOT NULL")
    List<PaymentFailures> findDcn();
}
