package uk.gov.hmcts.payment.api.v1.componenttests.backdoors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.payment.api.v1.model.PaymentOld;
import uk.gov.hmcts.payment.api.v1.model.PaymentOld.PaymentOldBuilder;
import uk.gov.hmcts.payment.api.v1.model.PaymentRepository;

@Component
public class DbBackdoor {
    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentOld create(PaymentOldBuilder paymentDetails) {
        return paymentRepository.save(paymentDetails.build());
    }
}
