package uk.gov.hmcts.payment.api.model;

import org.springframework.data.repository.CrudRepository;

public interface PaymentFeeLinkRepository extends CrudRepository<PaymentFeeLink, Integer> {

    <S extends PaymentFeeLink> S save(S entity);

}
