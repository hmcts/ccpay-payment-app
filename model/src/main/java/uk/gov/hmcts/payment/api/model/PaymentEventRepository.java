package uk.gov.hmcts.payment.api.model;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PaymentEventRepository extends AbstractRepository<PaymentEvent, String> {

    @Override
    default String getEntityName() {
        return PaymentEventRepository.class.getName();
    }
}
