package uk.gov.hmcts.payment.api.model;

import java.util.Date;
import java.util.List;

public interface CardPaymentService<T, ID> {

    T create(int amount, String paymentReference, String description, String returnUrl,
             String ccdCaseNumber, String caseReference, String currency, String siteId, String serviceType, List<Fee> fees);

    T retrieve(ID id);

    void cancel(ID id);

    void refund(ID id, int amount, int refundAmountAvailabie);

    List<T> search(Date startDate, Date endDate);
}
