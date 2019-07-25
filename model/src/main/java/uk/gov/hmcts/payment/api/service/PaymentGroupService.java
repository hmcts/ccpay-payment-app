package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.util.List;

public interface PaymentGroupService<T, ID> {

    T findByPaymentGroupReference(String paymentGroupReference);

    T addNewFeeWithPaymentGroup(PaymentFeeLink feeLink);

    T addNewFeetoExistingPaymentGroup(List<PaymentFee> fee, String PaymentGroupReference);

    List<T> search(String ccdCaseNumber);
}
