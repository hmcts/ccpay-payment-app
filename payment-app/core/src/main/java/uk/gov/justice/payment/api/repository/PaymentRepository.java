package uk.gov.justice.payment.api.repository;


import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.payment.api.model.PaymentDetails;


public interface PaymentRepository extends CrudRepository<PaymentDetails, Integer>, QueryDslPredicateExecutor<PaymentDetails> {

    <S extends PaymentDetails> S save(S entity);

    Iterable<PaymentDetails> findAll();

    PaymentDetails findByPaymentId(String paymentId);
}
