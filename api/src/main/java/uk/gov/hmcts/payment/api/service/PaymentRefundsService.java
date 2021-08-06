package uk.gov.hmcts.payment.api.service;

import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.payment.api.dto.PaymentRefundRequest;
import uk.gov.hmcts.payment.api.dto.RefundResponse;

public interface PaymentRefundsService {

    RefundResponse CreateRefund(PaymentRefundRequest paymentRefundRequest, MultiValueMap<String, String> headers);
}
