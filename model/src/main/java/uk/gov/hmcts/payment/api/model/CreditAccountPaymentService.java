package uk.gov.hmcts.payment.api.model;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;

import java.util.List;

public interface CreditAccountPaymentService<T, ID> {

    T create(Payment creditAccount, List<Fee> fees, String paymentGroupReference) throws CheckDigitException;

    T retrieveByPaymentGroupReference(ID id);

    T retrieveByPaymentReference(ID id);
}
