package uk.gov.hmcts.payment.api.v1.model;


import org.springframework.data.repository.CrudRepository;
import java.util.Optional;


public interface PaymentRepository extends CrudRepository<PaymentOld, Integer> {

    <S extends PaymentOld> S save(S entity);

    Optional<PaymentOld> findByUserIdAndId(String userId, Integer id);


}
