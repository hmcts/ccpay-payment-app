package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentFailureRepository extends CrudRepository<PaymentFailures, Integer> {

    Optional<PaymentFailures> findByFailureReference(String failureReference);
    Optional<List<PaymentFailures>> findByPaymentReferenceOrderByFailureEventDateTimeDesc(String paymentReference);
    Optional<List<PaymentFailures>> findByPaymentReference(String paymentReference);
    long deleteByFailureReference(String failureReference);
    List<PaymentFailures> findAll();
}
