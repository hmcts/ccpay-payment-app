package uk.gov.hmcts.payment.api.service;

public interface PaymentService<T, ID> {

    T retrieve(ID id);
}
