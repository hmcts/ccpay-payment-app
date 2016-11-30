package uk.gov.justice.payment.api.services;

public interface PaymentService<T> {

    T create(String serviceId, String applicationReference, Integer amount, String email, String paymentReference, String description, String returnUrl);

    T retrieve(String serviceId, String id);

    void cancel(String serviceId, String paymentId);

    void refund(String serviceId, String paymentId, Integer amount, Integer refundAmountAvailabie);
}
