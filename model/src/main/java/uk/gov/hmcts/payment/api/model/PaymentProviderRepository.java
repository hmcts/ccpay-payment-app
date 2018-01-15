package uk.gov.hmcts.payment.api.model;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PaymentProviderRepository extends AbstractRepository<PaymentProvider, String> {

    @Override
    default String getEntityName() {
        return PaymentProvider.class.getName();
    }
}
