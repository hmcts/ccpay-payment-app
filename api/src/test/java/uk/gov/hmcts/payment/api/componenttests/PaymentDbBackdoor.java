package uk.gov.hmcts.payment.api.componenttests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink.PaymentFeeLinkBuilder;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;
import uk.gov.hmcts.payment.api.v1.model.exceptions.PaymentNotFoundException;

@Component
public class PaymentDbBackdoor {

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    public PaymentFeeLink create(PaymentFeeLinkBuilder paymentFeeLink) {
        return paymentFeeLinkRepository.save(paymentFeeLink.build());
    }

    public PaymentFeeLink findByReference(String reference) {
        return paymentFeeLinkRepository.findByPaymentReference(reference).orElseThrow(PaymentNotFoundException::new);
    }


}
