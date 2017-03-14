package uk.gov.hmcts.payment.api.componenttests.backdoors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.Payment.PaymentBuilder;
import uk.gov.hmcts.payment.api.model.PaymentRepository;

@Component
public class DbBackdoor {
    @Autowired
    private PaymentRepository paymentRepository;

    public Payment create(PaymentBuilder paymentDetails) {
        return paymentRepository.save(paymentDetails.build());
    }
}
