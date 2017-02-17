package uk.gov.justice.payment.api.model;

public interface PaymentService<T> {

    T create(String serviceId, int amount, String reference, String description, String returnUrl);

    T retrieve(String serviceId, Integer id);

    void cancel(String serviceId, Integer id);

    void refund(String serviceId, Integer id, int amount, int refundAmountAvailabie);
}
