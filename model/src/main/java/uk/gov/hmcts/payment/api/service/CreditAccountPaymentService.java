package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.Payment;

import java.util.Date;
import java.util.List;

public interface CreditAccountPaymentService<T, ID> {

    T create(Payment creditAccount, List<PaymentFee> fees, String paymentGroupReference) throws CheckDigitException;

    T retrieveByPaymentGroupReference(ID id);

    T retrieveByPaymentReference(ID id);

    List<T> search(Date startDate, Date endDate);

    void deleteByPaymentReference(String paymentReference);
}
