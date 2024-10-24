package uk.gov.hmcts.payment.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;

@Component
public class PaymentReference {

    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    public String getNext() {
        return paymentFeeLinkRepository.getNextPaymentReference();
    }

    @Autowired
    public PaymentReference(PaymentFeeLinkRepository paymentFeeLinkRepository) {
        this.paymentFeeLinkRepository = paymentFeeLinkRepository;
    }

}
