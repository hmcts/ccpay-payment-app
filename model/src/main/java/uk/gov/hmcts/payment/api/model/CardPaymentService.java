package uk.gov.hmcts.payment.api.model;

import java.util.List;

public interface CardPaymentService<T, ID> {

    T create(int amount, String paymentReference, String description, String returnUrl,
             String ccdCaseNumber, String caseReference, String currency, String siteId, List<Fee> fees);

    T retrieve(ID id);

    void cancel(ID id);

    void refund(ID id, int amount, int refundAmountAvailabie);
}
