package uk.gov.hmcts.payment.api.v1.model;


import org.springframework.data.repository.CrudRepository;
import java.util.Optional;


public interface PaymentRepository extends CrudRepository<Payment, Integer> {

    <S extends Payment> S save(S entity);

    Optional<Payment> findByUserIdAndId(String userId, Integer id);
}
