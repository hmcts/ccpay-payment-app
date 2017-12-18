package uk.gov.hmcts.payment.api.model;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PaymentStatusRepository extends AbstractRepository<PaymentStatus, String> {

    @Override
    default String getEntityName() {
        return PaymentStatus.class.getName();
    }
}
