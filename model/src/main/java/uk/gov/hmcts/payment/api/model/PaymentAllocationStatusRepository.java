package uk.gov.hmcts.payment.api.model;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PaymentAllocationStatusRepository extends AbstractRepository<PaymentAllocationStatus, String> {

    @Override
    default String getEntityName() {
        return PaymentAllocationStatus.class.getName();
    }
}
