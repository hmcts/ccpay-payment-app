package uk.gov.hmcts.payment.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;

import javax.annotation.PostConstruct;

@Component
public class PaymentReference {

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepositoryInstance;

    private static PaymentFeeLinkRepository paymentFeeLinkRepository;

    private static PaymentReference obj = null;

    @PostConstruct
    private void init() {
        paymentFeeLinkRepository = paymentFeeLinkRepositoryInstance;
    }

    private PaymentReference() {
    }

    public static PaymentReference getInstance() {
        if (obj == null) {
            obj = new PaymentReference();
        }

        return obj;
    }

    public String getNext() {
        return paymentFeeLinkRepository.getNextPaymentReference();
    }

}
