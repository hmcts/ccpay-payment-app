package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface PaymentFailureRepository extends CrudRepository<PaymentFailures, Integer> {

    Optional<PaymentFailures> findByFailureReference(String failureReference);
    Optional<List<PaymentFailures>> findByPaymentReference(String paymentReference);

}
