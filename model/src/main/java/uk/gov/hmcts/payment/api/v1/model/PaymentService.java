package uk.gov.hmcts.payment.api.v1.model;

public interface PaymentService<T, ID> {

    T create(int amount, String reference, String description, String returnUrl, String language);

    T retrieve(ID id);

    void cancel(ID id);

    void refund(ID id, int amount, int refundAmountAvailabie);
}
