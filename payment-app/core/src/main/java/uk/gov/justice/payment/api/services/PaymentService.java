package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.model.Payment;


public interface PaymentService extends PaymentSearchService {

    Payment create(String serviceId, Integer amount, String email, String applicationReference, String paymentReference, String description, String returnUrl);

    Payment findByPaymentId(String serviceId, String paymentId);

    void cancel(String serviceId, String paymentId);

    void refund(String serviceId, String paymentId, Integer amount, Integer refundAmountAvailabie);
}
