package uk.gov.hmcts.payment.api.model;


import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;


public interface PaymentRepository extends CrudRepository<Payment, Integer>, QueryDslPredicateExecutor<Payment> {

    <S extends Payment> S save(S entity);

    Optional<Payment> findByUserIdAndId(String userId, Integer id);
}
