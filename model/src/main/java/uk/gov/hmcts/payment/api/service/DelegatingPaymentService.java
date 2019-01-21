package uk.gov.hmcts.payment.api.service;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import uk.gov.hmcts.payment.api.model.PaymentFee;

import java.util.Date;
import java.util.List;

public interface DelegatingPaymentService<T, ID> {

    T create(String paymentReference, String description, String returnUrl, String ccdCaseNumber, String caseReference,
             String currency, String siteId, String serviceType, List<PaymentFee> fees, int amount,
             String serviceCallbackUrl, String channel, String provider) throws CheckDigitException;

    T retrieve(ID id);

    T retrieve(ID id, String paymentTargetService);

    List<T> search(Date startDate, Date endDate, String paymentMethod, String serviceName, String ccdCaseNumber, String pbaNumber);

}
