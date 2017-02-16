package uk.gov.justice.payment.api.model;


import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.justice.payment.api.model.Payment;

import java.util.Optional;


public interface PaymentRepository extends CrudRepository<Payment, Integer>, QueryDslPredicateExecutor<Payment> {

    <S extends Payment> S save(S entity);

    Iterable<Payment> findAll();

    Optional<Payment> findById(Integer id);
}
