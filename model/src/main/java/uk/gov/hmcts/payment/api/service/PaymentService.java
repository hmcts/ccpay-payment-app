package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.dto.PaymentSearchCriteria;
import uk.gov.hmcts.payment.api.dto.Reference;
import uk.gov.hmcts.payment.api.model.Payment;

import java.util.List;

public interface PaymentService<T, ID> {

    T retrieve(ID id);

    List<Reference> listInitiatedStatusPaymentsReferences();

    List<T> search(PaymentSearchCriteria searchCriteria);

    void updateTelephonyPaymentStatus(String reference, String status, String payload);

    Payment createPayment(Payment payment);
}
