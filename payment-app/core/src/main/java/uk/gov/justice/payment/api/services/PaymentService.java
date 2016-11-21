package uk.gov.justice.payment.api.services;

import uk.gov.justice.payment.api.model.PaymentDetails;


public interface PaymentService extends PaymentSearchService {

    PaymentDetails create(String serviceId, Integer amount, String email, String applicationReference, String paymentReference, String description, String returnUrl);

    PaymentDetails findByPaymentId(String serviceId, String paymentId);

    void cancel(String serviceId, String paymentId);

    void refund(String serviceId, String paymentId, Integer amount, Integer refundAmountAvailabie);
}
