package uk.gov.justice.payment.api.componenttests.backdoors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.payment.api.model.Payment;
import uk.gov.justice.payment.api.model.Payment.PaymentBuilder;
import uk.gov.justice.payment.api.model.PaymentHistoryEntry;
import uk.gov.justice.payment.api.repository.PaymentHistoryRepository;
import uk.gov.justice.payment.api.repository.PaymentRepository;

import static uk.gov.justice.payment.api.model.QPaymentHistoryEntry.paymentHistoryEntry;

@Component
public class DbBackdoor {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    public Payment create(PaymentBuilder paymentDetails) {
        return paymentRepository.save(paymentDetails.build());
    }

    public PaymentHistoryEntry getLastPaymentHistoryEntry() {
        return paymentHistoryRepository.findAll(paymentHistoryEntry.id.desc()).iterator().next();
    }
}
