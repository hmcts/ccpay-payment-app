package uk.gov.hmcts.payment.api.model;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PaymentMethodRepository extends AbstractRepository<PaymentMethod, String> {

    @Override
    default String getEntityName() {
        return PaymentMethod.class.getName();
    }


}
