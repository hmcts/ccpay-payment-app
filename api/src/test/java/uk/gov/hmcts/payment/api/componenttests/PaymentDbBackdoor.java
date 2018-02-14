package uk.gov.hmcts.payment.api.componenttests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink.PaymentFeeLinkBuilder;
import uk.gov.hmcts.payment.api.model.PaymentFeeLinkRepository;

@Component
public class PaymentDbBackdoor {

    @Autowired
    private PaymentFeeLinkRepository paymentFeeLinkRepository;

    public PaymentFeeLink create(PaymentFeeLinkBuilder paymentFeeLink) {
        return paymentFeeLinkRepository.save(paymentFeeLink.build());
    }


}
