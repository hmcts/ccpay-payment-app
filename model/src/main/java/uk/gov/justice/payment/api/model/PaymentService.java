package uk.gov.justice.payment.api.model;

public interface PaymentService<T, ID> {

    T create(int amount, String reference, String description, String returnUrl);

    T retrieve(ID id);

    void cancel(ID id);

    void refund(ID id, int amount, int refundAmountAvailabie);
}
