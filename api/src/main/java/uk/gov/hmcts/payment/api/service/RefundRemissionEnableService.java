
package uk.gov.hmcts.payment.api.service;

import java.util.List;
import java.util.Optional;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.domain.model.Roles;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;

public interface RefundRemissionEnableService {

    Boolean returnRefundEligible(Payment payment);
    Boolean returnRemissionEligible(PaymentFee fee);
     boolean isRolePresent(MultiValueMap<String, String> headers);

}

