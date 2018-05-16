package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.model.Fee;

import java.util.Date;
import java.util.List;

public interface CardPaymentService<T, ID> {

    T create(int amount, String paymentReference, String description, String returnUrl,
             String ccdCaseNumber, String caseReference, String currency, String siteId, String serviceType, List<Fee> fees) throws CheckDigitException;

    T retrieve(ID id);

    List<T> search(Date startDate, Date endDate, String type);
}
