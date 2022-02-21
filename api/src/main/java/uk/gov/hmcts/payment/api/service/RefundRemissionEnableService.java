
package uk.gov.hmcts.payment.api.service;

import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.PaymentGroupResponse;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;

public interface RefundRemissionEnableService {

    Boolean returnRefundEligible(Payment payment);
    Boolean returnRemissionEligible(PaymentFee fee);
     boolean isRolePresent(MultiValueMap<String, String> headers);

    PaymentGroupResponse checkRefundAgainstRemission(MultiValueMap<String, String> headers, PaymentGroupResponse paymentGroupResponse, String ccdCaseNumber);
}

