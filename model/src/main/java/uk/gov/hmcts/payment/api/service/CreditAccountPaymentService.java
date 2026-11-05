package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.model.PaymentFee;
import uk.gov.hmcts.payment.api.model.Payment;

import java.util.Date;
import java.util.List;

public interface CreditAccountPaymentService<T, I> {

    T create(Payment creditAccount, List<PaymentFee> fees, String paymentGroupReference) throws CheckDigitException;

    T retrieveByPaymentGroupReference(I id);

    T retrieveByPaymentReference(I id);

    List<T> search(Date startDate, Date endDate);

    void deleteByPaymentReference(String paymentReference);

    Payment createOrUpdate( Payment  payment) throws CheckDigitException;
}
