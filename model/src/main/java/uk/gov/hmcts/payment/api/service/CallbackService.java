package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

public interface CallbackService {
    void callback(PaymentFeeLink paymentFeeLink, Payment payment);

}
