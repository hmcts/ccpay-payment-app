package uk.gov.hmcts.payment.api.service;

import uk.gov.hmcts.payment.api.dto.Reference;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService<T, ID> {

    T retrieve(ID id);

    List<Reference> listInitiatedStatusPaymentsReferences();

    List<T> search(LocalDateTime startDate, LocalDateTime endDate, String paymentMethod, String serviceType, String ccdCaseNumber);
}
