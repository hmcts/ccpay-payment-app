package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.PaymentFee;

public interface PaymentGroupService<T, ID> {

    T findByPaymentGroupReference(String paymentGroupReference);

    T addNewFeeWithPaymentGroup(PaymentFee fee, String PaymentGroupReference);

    T addNewFeetoExistingPaymentGroup(PaymentFee fee, String PaymentGroupReference);
}
