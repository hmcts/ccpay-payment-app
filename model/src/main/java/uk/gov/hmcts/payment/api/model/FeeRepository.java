package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;

public interface FeeRepository  extends CrudRepository<PaymentFee, Integer> {

    <S extends PaymentFee> S save(S entity);

}
