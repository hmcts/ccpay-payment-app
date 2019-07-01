package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

public interface PaymentGroupService<T, ID> {

    T findByPaymentGroupReference(String paymentGroupReference);

    T addNewFeeWithPaymentGroup(PaymentFeeLink feeLink);

    T addNewFeetoExistingPaymentGroup(PaymentFee fee, String PaymentGroupReference);
}
