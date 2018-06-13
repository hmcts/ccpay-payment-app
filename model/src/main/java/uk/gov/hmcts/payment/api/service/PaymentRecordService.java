package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.util.List;

public interface PaymentRecordService<T, ID> {

    T recordPayment(Payment recordPayment, List<PaymentFee> fees, String paymentGroupReference) throws CheckDigitException;
}
