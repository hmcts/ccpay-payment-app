package uk.gov.justice.payment.api.model;

public interface PaymentService<T> {

    T create(String serviceId, String applicationReference, int amount, String email, String paymentReference, String description, String returnUrl);

    T retrieve(String serviceId, String id);

    void cancel(String serviceId, String paymentId);

    void refund(String serviceId, String paymentId, int amount, int refundAmountAvailabie);
}
