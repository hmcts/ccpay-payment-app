package uk.gov.justice.payment.api.repository;

import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.payment.api.domain.PaymentDetails;


public interface PaymentRepository extends CrudRepository <PaymentDetails, Integer> {

    <S extends PaymentDetails> S  save(S entity);
    Iterable<PaymentDetails> findAll();

    PaymentDetails findByPaymentId(String paymentId);

    //Iterable<PaymentDetails> findAll(BooleanExpression criteria);

}
