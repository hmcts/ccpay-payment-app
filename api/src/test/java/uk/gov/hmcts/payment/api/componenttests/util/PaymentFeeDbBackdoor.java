package uk.gov.hmcts.payment.api.componenttests.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentFeeNotFoundException;

@Component
public class PaymentFeeDbBackdoor {

    @Autowired
    private PaymentFeeRepository paymentFeeRepository;

    public PaymentFee findByPaymentLinkId(Integer id) {
        return paymentFeeRepository.findByPaymentLinkId(id).orElseThrow(PaymentFeeNotFoundException::new);
    }
}
