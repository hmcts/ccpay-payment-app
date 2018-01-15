package uk.gov.hmcts.payment.api.model;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface PaymentChannelRepository extends AbstractRepository<PaymentChannel, String> {

    @Override
    default String getEntityName() {
        return PaymentChannel.class.getName();
    }
}
