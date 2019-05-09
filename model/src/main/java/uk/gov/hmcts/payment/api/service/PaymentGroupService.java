package uk.gov.hmcts.payment.api.service;

public interface PaymentGroupService<T, ID> {

    T findByPaymentGroupReference(String paymentGroupReference);
}
