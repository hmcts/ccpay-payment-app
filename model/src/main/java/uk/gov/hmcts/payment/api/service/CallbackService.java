package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

public interface CallbackService {

    String FEATURE = "payment-callback-service";

    void callback(PaymentFeeLink paymentFeeLink, Payment payment);

}
