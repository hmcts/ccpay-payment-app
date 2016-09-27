package uk.gov.justice.payment.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import uk.gov.justice.payment.api.domain.PaymentDetails;


public interface PaymentRepository extends CrudRepository <PaymentDetails, Integer>{

    <S extends PaymentDetails> S  save(S entity);
    Iterable<PaymentDetails> findAll();
}
