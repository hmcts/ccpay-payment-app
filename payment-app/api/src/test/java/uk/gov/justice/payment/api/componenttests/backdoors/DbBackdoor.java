package uk.gov.justice.payment.api.componenttests.backdoors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.payment.api.model.PaymentDetails;
import uk.gov.justice.payment.api.model.PaymentDetails.PaymentDetailsBuilder;
import uk.gov.justice.payment.api.repository.PaymentRepository;

@Component
public class DbBackdoor {
    @Autowired
    private PaymentRepository paymentRepository;

    public PaymentDetails create(PaymentDetailsBuilder paymentDetails) {
        return paymentRepository.save(paymentDetails.build());
    }
}
