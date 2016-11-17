package uk.gov.justice.payment.api.services;

import lombok.NonNull;
import uk.gov.justice.payment.api.model.PaymentDetails;

import java.util.List;


public interface PaymentService {

    PaymentDetails create(String serviceId, Integer amount, String email, String applicationReference, String paymentReference, String description, String returnUrl);

    PaymentDetails findByPaymentId(String serviceId, String paymentId);

    List<PaymentDetails> search(String serviceId, PaymentSearchCriteria searchCriteria);

    void cancel(String serviceId, String paymentId);
}
